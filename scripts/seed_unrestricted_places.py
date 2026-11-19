#!/usr/bin/env python3
"""Explicit fictional unrestricted venues; never run at application startup."""
import argparse
import datetime as dt
import json
import math
import os
from pathlib import Path
import runpy

ROOT = Path(__file__).resolve().parents[1]
DEFAULT_FILE = ROOT / 'scripts/fixtures/unrestricted_places.json'
h = runpy.run_path(str(ROOT / 'scripts/demo_seed.py'))


def load_fixture(path):
    data = json.loads(Path(path).read_text())
    if data.get('coordinateType') != 'FICTIONAL_SEONGSU_DEMO':
        raise ValueError('Only explicitly fictional demonstration coordinates are accepted')
    places = data['places']
    if not places or len({p['sourceId'] for p in places}) != len(places) or len({p['name'] for p in places}) != len(places):
        raise ValueError('Empty or duplicate source IDs/names')
    for index, p in enumerate(places, 1):
        if not p['name'].strip() or len(p['name']) > 100 or p['category'] not in ('CAFE', 'FOOD') or p['originalCondition'] != '없음':
            raise ValueError('Unreviewed name/category/condition')
        if not all(math.isfinite(p[k]) for k in ('latitude', 'longitude')) or not (37.54 <= p['latitude'] <= 37.55 and 127.05 <= p['longitude'] <= 127.06):
            raise ValueError('Coordinate outside the Seongsu demo area')
        p['businessNumber'] = str(9900010000 + index)
        p['index'] = index
    return data


def unrestricted_conditions():
    area = {'allowed': True, 'maxWeightKg': None, 'equipmentCodes': []}
    return {'petsAllowed': True, 'indoor': dict(area), 'outdoor': dict(area),
            'effectiveFrom': dt.datetime.now(h['SEOUL']).date().isoformat(),
            'notice': '시연용 가상 장소입니다. 반려견 체중·견종·준비물·예방접종 제한이 없습니다.',
            'changeReason': '제한 없는 초록색 입장 가능 시연', 'vaccinationConfirmed': False}


def protected(existing_ids):
    ids = ','.join(map(str, existing_ids)) or '-1'
    queries = {'stores': f'SELECT * FROM store WHERE store_id IN ({ids}) ORDER BY store_id',
               'policies': f'SELECT * FROM store_policy WHERE store_id IN ({ids}) ORDER BY policy_id',
               'legacyPolicies': 'SELECT * FROM place_entry_policy ORDER BY place_id',
               'dogs': 'SELECT * FROM dog ORDER BY dog_id',
               'qr': 'SELECT * FROM store_qr ORDER BY qr_id',
               'eventStores': 'SELECT * FROM event_store ORDER BY store_id',
               'users': 'SELECT * FROM app_user ORDER BY user_id',
               'visits': 'SELECT * FROM visit ORDER BY visit_id',
               'stamps': 'SELECT * FROM event_stamp ORDER BY stamp_id',
               'draws': 'SELECT * FROM draw_entry ORDER BY draw_entry_id',
               'publicPlaces': 'SELECT * FROM public_facility ORDER BY facility_id'}
    # Only legacy policies for pre-existing places are protected, excluding new STORE IDs.
    queries['legacyPolicies'] = f"SELECT * FROM place_entry_policy WHERE place_id NOT LIKE 'STORE:%' OR place_id IN (SELECT CONCAT('STORE:',store_id) FROM store WHERE store_id IN ({ids})) ORDER BY place_id"
    return {key: h['sql']("SELECT md5(COALESCE(json_agg(t)::text,'[]')) FROM (" + query + ') t') for key, query in queries.items()}


def apply(data, env_file):
    h['load_env'](env_file)
    q, sql, rows = h['quote'], h['sql'], h['rows']
    version = int(sql('SELECT version FROM flyway_schema_history WHERE success ORDER BY installed_rank DESC LIMIT 1'))
    if version < 23: raise RuntimeError('V23 or later is required')
    owner = h['one']("SELECT user_id FROM app_user WHERE login_id='owner.demo@pawfit.example' AND phone_number='01099000002' AND role='BUSINESS'")
    if not owner: raise RuntimeError('Existing demonstration business account is required')
    doc = h['one']("SELECT document_url FROM store_verification WHERE submitted_by=" + str(owner['user_id']) + " AND store_id=6 AND status='APPROVED' ORDER BY verification_id LIMIT 1")
    if not doc: raise RuntimeError('Existing fictional demonstration evidence is required')
    before_ids = [r['store_id'] for r in rows('SELECT store_id FROM store ORDER BY store_id')]
    before = protected(before_ids)
    folder = ROOT / '.demo/unrestricted-places'; folder.mkdir(mode=0o700, parents=True, exist_ok=True)
    backup = folder / ('before-' + dt.datetime.now(dt.timezone.utc).strftime('%Y%m%d-%H%M%S-%f') + '.json')
    snapshot = {'existingStoreIds': before_ids, 'protectedHashes': before,
                'existingSeed': rows('SELECT * FROM demo_seed_manifest WHERE seed_key=' + q(data['seedKey'])), 'fixture': data}
    with os.fdopen(os.open(backup, os.O_WRONLY | os.O_CREAT | os.O_EXCL, 0o600), 'w') as f:
        json.dump(snapshot, f, ensure_ascii=False, indent=2)
    source_text = '시연용 가상 장소: 반려견 동반 가능, 체중·견종·준비물·예방접종 제한 없음. 실제 업체 정보가 아닙니다.'
    statement = f"""BEGIN;
SELECT pg_advisory_xact_lock(707019);
DO $seed$ DECLARE r RECORD; sid BIGINT; pid BIGINT; BEGIN
FOR r IN SELECT * FROM jsonb_to_recordset({q(json.dumps(data['places'], ensure_ascii=False))}::jsonb)
 AS x("sourceId" TEXT,name TEXT,category TEXT,latitude NUMERIC,longitude NUMERIC,"businessNumber" TEXT,index INT) LOOP
 SELECT entity_id::bigint INTO sid FROM demo_seed_manifest
 WHERE seed_key={q(data['seedKey'])} AND entity_type='STORE' AND metadata::jsonb->>'sourceId'=r."sourceId";
 IF sid IS NULL THEN
  IF EXISTS(SELECT 1 FROM store WHERE name=r.name) THEN RAISE EXCEPTION 'Review existing store name: %',r.name; END IF;
  IF EXISTS(SELECT 1 FROM store_verification WHERE business_number=r."businessNumber") THEN RAISE EXCEPTION 'Reserved demo identity already used'; END IF;
  INSERT INTO store(name,address,latitude,longitude,verification_status,visibility,region_id,category,description)
   VALUES(r.name,'서울특별시 성동구 성수동 시연용 가상 위치 '||r.index,r.latitude,r.longitude,'VERIFIED','VISIBLE',11,r.category,'시연용 가상 장소입니다. 실제 업체·주소가 아닙니다.') RETURNING store_id INTO sid;
  INSERT INTO store_manager(store_id,user_id) VALUES(sid,{owner['user_id']});
  INSERT INTO store_verification(store_id,submitted_by,business_number,business_name,representative_name,document_url,status,review_note,reviewed_at)
   VALUES(sid,{owner['user_id']},r."businessNumber",r.name,'시연 담당자',{q(doc['document_url'])},'APPROVED','시연용 가상 업체, 실제 사업자 인증이 아닙니다.',now());
  INSERT INTO store_policy(store_id,version,source_text,conditions_json)
   VALUES(sid,1,{q(source_text)},{q(json.dumps(unrestricted_conditions(),ensure_ascii=False))}) RETURNING policy_id INTO pid;
  INSERT INTO place_entry_policy(place_id,pets_allowed,max_weight_kg,source_text,confirmed_on,details_complete,max_weight_exclusive)
   VALUES(CONCAT('STORE:',sid),TRUE,NULL,{q(source_text)},(CURRENT_TIMESTAMP AT TIME ZONE 'Asia/Seoul')::date,TRUE,FALSE);
  INSERT INTO demo_seed_manifest(seed_key,entity_type,entity_id,metadata)
   VALUES({q(data['seedKey'])},'STORE',sid::text,jsonb_build_object('sourceId',r."sourceId",'label',r.name,'sourceUrl',{q(data['sourceUrl'])},'coordinateType','FICTIONAL_SEONGSU_DEMO','policyId',pid)::text);
 END IF;
END LOOP; END $seed$;
COMMIT;"""
    sql(statement)
    result = rows("SELECT s.store_id,s.name,s.category,s.latitude,s.longitude,s.visibility,s.verification_status,p.pets_allowed,p.details_complete,p.max_weight_kg FROM demo_seed_manifest m JOIN store s ON s.store_id=m.entity_id::bigint JOIN place_entry_policy p ON p.place_id=CONCAT('STORE:',s.store_id) WHERE m.seed_key=" + q(data['seedKey']) + " AND m.entity_type='STORE' ORDER BY s.store_id")
    assert len(result) == len(data['places'])
    assert all(r['pets_allowed'] and r['details_complete'] and r['max_weight_kg'] is None and r['visibility']=='VISIBLE' and r['verification_status']=='VERIFIED' for r in result)
    assert protected(before_ids) == before, 'Existing data changed; inspect snapshot'
    report = {'seedKey': data['seedKey'], 'createdAt': dt.datetime.now(dt.timezone.utc).isoformat(),
              'newStoreCount': len(rows('SELECT store_id FROM store')) - len(before_ids),
              'count': len(result), 'places': result, 'backupFile': str(backup), 'existingDataPreserved': True}
    (folder / 'report.json').write_text(json.dumps(report, ensure_ascii=False, indent=2))
    print(json.dumps(report, ensure_ascii=False, indent=2))


def main():
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument('action', choices=['plan', 'apply']); parser.add_argument('--file', default=str(DEFAULT_FILE)); parser.add_argument('--env-file')
    args = parser.parse_args(); data = load_fixture(args.file)
    if args.action == 'plan': print(json.dumps(data, ensure_ascii=False, indent=2))
    else: apply(data, args.env_file)


if __name__ == '__main__': main()
