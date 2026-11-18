#!/usr/bin/env python3
"""Apply explicitly reviewed admission evidence, separately from catalog collection."""
import argparse
import datetime as dt
import json
from pathlib import Path
import runpy

ROOT = Path(__file__).resolve().parents[1]
helpers = runpy.run_path(str(ROOT / 'scripts/demo_seed.py'))
IMPORTER = 'confirmed-admission-v1'


def choices(value):
    return json.loads(value) if isinstance(value, str) else (value or [])


def team_policy(row):
    size = choices(row.get('크기 제한'))
    equipment = choices(row.get('준비물'))
    codes = {'캐리어': 'CARRIER', '리드줄': 'LEASH', '유모차': 'STROLLER', '입마개': 'MUZZLE', '기저귀': 'BELLY_BAND'}
    # Multiple choices do not establish whether requirements are AND or OR.
    required = [codes[equipment[0]]] if len(equipment) == 1 and equipment[0] in codes else []
    return {'max_weight_kg': 7 if '7kg 미만' in size else None,
            'max_weight_exclusive': '7kg 미만' in size, 'equipment': required}


def confirmed_kto(row):
    return bool(row.get('items')) and any(
        i.get('acmpyTypeCd') in ('전구역 동반가능', '일부구역 동반가능')
        for i in row['items'])


def prepare(folder):
    team = json.loads((folder / 'notion-researched-places.json').read_text())
    geo = {r['pageId']: r for r in json.loads((folder / 'notion-geocoded-places.json').read_text())}
    result = []
    for row in team:
        g = geo[row['id']]
        if not g.get('coordinate'): raise RuntimeError('미검증 좌표: ' + row['업체명'])
        c = g['coordinate']; policy = team_policy(row)
        fields = '; '.join(k + ': ' + (', '.join(choices(v)) if k != '메모' else v)
                           for k, v in row.items() if v and k in ('체중 제한', '예방접종', '준비물', '크기 제한', '견종 제한', '메모'))
        source = f"팀 직접 조사: 반려견 동반 가능. {fields or '세부 조건 미확인'}. 원문: {row['url']}"
        if 'OSM' in c['source']: source += ' / 좌표 © OpenStreetMap contributors, ODbL 1.0'
        result.append({'source_code': 'TEAM_RESEARCH', 'external_id': row['id'], 'name': row['업체명'],
                       'address': row['주소'], 'category': 'CAFE' if any('카페' in s or s == 'CAFE' for s in choices(row['분류'])) else 'FOOD',
                       'latitude': c['latitude'], 'longitude': c['longitude'], 'source_text': source[:2000],
                       'evidence': json.dumps({'importer': IMPORTER, 'notion': row, 'coordinates': c}, ensure_ascii=False), **policy})
    for row in json.loads((folder / 'seoul-pet-admission-confirmed-cache.json').read_text()):
        if not confirmed_kto(row): continue
        info = '; '.join(f'{k}: {v}' for item in row['items'] for k, v in item.items() if v and k != 'contentid')
        result.append({'source_code': 'KTO_PET_TOUR', 'external_id': row['externalId'],
                       'source_text': ('한국관광공사 반려동물 동반 상세: ' + info)[:2000],
                       'evidence': json.dumps({'importer': IMPORTER, 'official': row}, ensure_ascii=False),
                       'max_weight_kg': None, 'max_weight_exclusive': False, 'equipment': []})
    keys = [(r['source_code'], r['external_id']) for r in result]
    if len(keys) != len(set(keys)): raise RuntimeError('원본 ID 중복')
    return result


def apply(args):
    folder = ROOT / '.demo'; incoming = prepare(folder)
    helpers['load_env'](args.env_file)
    q, sql, rows = helpers['quote'], helpers['sql'], helpers['rows']
    if int(sql('SELECT version FROM flyway_schema_history WHERE success ORDER BY installed_rank DESC LIMIT 1')) < 23:
        raise RuntimeError('배포 서버 V23 적용 후 실행하세요.')
    # A team place may already exist in the public catalog. Require review rather than guess a merge.
    existing = rows("SELECT facility_id,name,address,source_code,external_id FROM public_facility")
    norm = lambda s: ''.join(c for c in s.lower() if c.isalnum())
    for item in incoming:
        if item['source_code'] != 'TEAM_RESEARCH': continue
        matches = [r for r in existing if r['source_code'] != 'TEAM_RESEARCH' and norm(r['name']) == norm(item['name'])]
        if matches: raise RuntimeError('동일 이름 중복 검토 필요: ' + item['name'])
    timestamp = dt.datetime.now(dt.timezone.utc).strftime('%Y%m%d-%H%M%S-%f')
    backup = folder / f'admission-backup-{timestamp}.json'
    backup.write_text(json.dumps({'facilities': rows('SELECT * FROM public_facility'),
                                 'policies': rows('SELECT * FROM place_entry_policy'),
                                 'rules': rows('SELECT * FROM place_entry_rule')}, ensure_ascii=False, indent=2))
    statement = f"""BEGIN;
    SELECT pg_advisory_xact_lock(17021701);
    CREATE TEMP TABLE incoming_admission ON COMMIT DROP AS SELECT * FROM jsonb_to_recordset({q(json.dumps(incoming,ensure_ascii=False))}::jsonb)
      AS x(source_code TEXT,external_id TEXT,name TEXT,address TEXT,category TEXT,latitude NUMERIC,longitude NUMERIC,
           source_text TEXT,evidence TEXT,max_weight_kg NUMERIC,max_weight_exclusive BOOLEAN,equipment JSONB);
    INSERT INTO public_facility(source_code,external_id,name,address,region_id,category,latitude,longitude,visible,admission_evidence)
      SELECT source_code,external_id,name,address,11,category,latitude,longitude,TRUE,evidence FROM incoming_admission WHERE source_code='TEAM_RESEARCH'
      ON CONFLICT(source_code,external_id) DO UPDATE SET name=EXCLUDED.name,address=EXCLUDED.address,category=EXCLUDED.category,
        latitude=EXCLUDED.latitude,longitude=EXCLUDED.longitude,admission_evidence=EXCLUDED.admission_evidence
      WHERE NOT public_facility.operator_corrected;
    UPDATE public_facility f SET admission_evidence=i.evidence FROM incoming_admission i
      WHERE f.source_code=i.source_code AND f.external_id=i.external_id AND NOT f.operator_corrected;
    INSERT INTO place_entry_policy(place_id,pets_allowed,max_weight_kg,source_text,confirmed_on,details_complete,max_weight_exclusive)
      SELECT CONCAT('PUBLIC:',f.facility_id),TRUE,i.max_weight_kg,i.source_text,(CURRENT_TIMESTAMP AT TIME ZONE 'Asia/Seoul')::date,FALSE,i.max_weight_exclusive
      FROM incoming_admission i JOIN public_facility f USING(source_code,external_id) WHERE NOT f.operator_corrected
      ON CONFLICT(place_id) DO NOTHING;
    INSERT INTO place_entry_rule(place_id,rule_type,rule_code)
      SELECT p.place_id,'EQUIPMENT',code FROM incoming_admission i JOIN public_facility f USING(source_code,external_id)
      JOIN place_entry_policy p ON p.place_id=CONCAT('PUBLIC:',f.facility_id) AND p.source_text=i.source_text
      CROSS JOIN LATERAL jsonb_array_elements_text(i.equipment) code WHERE NOT f.operator_corrected
      ON CONFLICT DO NOTHING;
    COMMIT;"""
    sql(statement)
    applied = rows("SELECT f.facility_id,f.source_code,f.external_id,f.name,f.address,f.category,f.latitude,f.longitude,f.visible,p.pets_allowed,p.details_complete,p.max_weight_kg,p.max_weight_exclusive,(c.place_id IS NOT NULL) AS catalog_visible FROM public_facility f LEFT JOIN place_entry_policy p ON p.place_id=CONCAT('PUBLIC:',f.facility_id) LEFT JOIN place_catalog c ON c.place_id=p.place_id WHERE f.admission_evidence IS NOT NULL ORDER BY f.source_code,f.facility_id")
    report = {'appliedAt': dt.datetime.now(dt.timezone.utc).isoformat(), 'backupFile': str(backup),
              'teamCount': sum(r['source_code'] == 'TEAM_RESEARCH' for r in applied),
              'officialCount': sum(r['source_code'] == 'KTO_PET_TOUR' for r in applied), 'places': applied}
    (folder / 'admission-import-report.json').write_text(json.dumps(report, ensure_ascii=False, indent=2))
    print(json.dumps({k:v for k,v in report.items() if k != 'places'},ensure_ascii=False))


if __name__ == '__main__':
    parser=argparse.ArgumentParser(description=__doc__);parser.add_argument('--env-file');parser.add_argument('--apply',action='store_true');args=parser.parse_args()
    if not args.apply: parser.error('검토된 근거와 좌표 파일을 준비한 뒤 --apply로 실행하세요.')
    apply(args)
