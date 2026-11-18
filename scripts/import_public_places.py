#!/usr/bin/env python3
"""Collect and explicitly publish a reviewed regional KTO catalog; never runs on startup."""
import argparse
import datetime as dt
import hashlib
import json
import math
import os
from pathlib import Path
import runpy
import time
import urllib.error
import urllib.parse
import urllib.request

ROOT=Path(__file__).resolve().parents[1]
helpers=runpy.run_path(str(ROOT/'scripts/demo_seed.py'))
ENDPOINT='https://apis.data.go.kr/B551011/KorPetTourService2/areaBasedList2'
CATEGORIES={'12':'ATTRACTION','14':'CULTURE','15':'EVENT','28':'LEISURE','32':'LODGING','38':'SHOPPING','39':'FOOD'}

def service_key():
    value=os.environ.get('DATA_GO_KR_SERVICE_KEY')
    if value:return value
    path=ROOT/'.env'
    if path.exists():
        for line in path.read_text().splitlines():
            if line.startswith('DATA_GO_KR_SERVICE_KEY='):return line.split('=',1)[1].strip().strip('"').strip("'")
    raise RuntimeError('DATA_GO_KR_SERVICE_KEY가 필요합니다.')

def normalize(item,region):
    ident=str(item.get('contentid','')).strip();name=str(item.get('title','')).strip()
    if not ident.isdigit() or len(ident)>80 or not name:return None,'식별자/이름 누락'
    try:lat,lon=float(item['mapy']),float(item['mapx'])
    except (KeyError,ValueError,TypeError):return None,'좌표 누락'
    if not math.isfinite(lat) or not math.isfinite(lon) or not (32<=lat<=39 and 124<=lon<=132):return None,'유효한 국내 좌표 아님'
    code=str(item.get('lDongRegnCd','')).strip()
    if code and code!=region:return None,'요청 지역과 원본 지역 불일치'
    address=(str(item.get('addr1',''))+' '+str(item.get('addr2',''))).strip()
    if region=='11' and not address.startswith(('서울','서울특별시')):return None,'서울 주소 확인 필요'
    image=str(item.get('firstimage') or item.get('firstimage2') or '').strip()
    if image.startswith('http://tong.visitkorea.or.kr/'):image=image.replace('http://','https://',1)
    parsed=urllib.parse.urlparse(image)
    if parsed.scheme not in ('http','https') or not parsed.hostname:image=None
    category=CATEGORIES.get(str(item.get('contenttypeid','')))
    if str(item.get('contenttypeid',''))=='39' and item.get('lclsSystm3') in ('FD050100','FD050200'):
        category='CAFE'  # KTO's current classification: cafe / teahouse.
    return {'external_id':ident,'name':name[:150],'address':address[:255] or None,
            'category':category,'latitude':round(lat,7),'longitude':round(lon,7),
            'region_id':int(region),'phone':str(item.get('tel',''))[:80], 'image_url':image[:2000] if image else None},None

def collect(args):
    key=service_key();raw=[];expected=None
    for page in range(1,201):
        query={'serviceKey':key,'MobileOS':'ETC','MobileApp':'PawFit','_type':'json','numOfRows':100,'pageNo':page,'arrange':'A','lDongRegnCd':args.region_code}
        response=None
        for attempt in range(3):
            try:
                with urllib.request.urlopen(ENDPOINT+'?'+urllib.parse.urlencode(query),timeout=30) as stream:response=json.load(stream)['response']
                break
            except Exception:
                if attempt==2:raise RuntimeError(f'공공데이터 {page}페이지 수집 실패 (키·URL 비출력)') from None
                time.sleep(attempt+1)
        if response.get('header',{}).get('resultCode') not in ('0000','00'):raise RuntimeError('공공데이터 결과 오류')
        body=response['body'];total=int(body['totalCount']);expected=total if expected is None else expected
        if expected!=total:raise RuntimeError('수집 중 전체 건수가 바뀌었습니다. 다시 수집하세요.')
        items=body.get('items',{});items=items.get('item',[]) if isinstance(items,dict) else []
        if isinstance(items,dict):items=[items]
        if not items and len(raw)<total:raise RuntimeError('완료 전 빈 페이지가 반환됐습니다.')
        raw.extend(items);print(f'수집 {page}페이지: {len(raw)}/{total}',flush=True)
        if len(raw)>=total:break
    if len(raw)!=expected:raise RuntimeError('원본 전체 건수와 수집 건수가 다릅니다.')
    accepted={};rejected=[];duplicates=[]
    for item in raw:
        row,reason=normalize(item,args.region_code)
        if row is None:rejected.append({'external_id':str(item.get('contentid','')),'name':item.get('title'),'reason':reason});continue
        if row['external_id'] in accepted:duplicates.append(row['external_id'])
        accepted[row['external_id']]=row
    result={'collectedAt':dt.datetime.now(dt.timezone.utc).isoformat(),'regionCode':args.region_code,'apiTotalCount':expected,
            'accepted':list(accepted.values()),'rejected':rejected,'duplicateIds':duplicates,'raw':raw}
    path=Path(args.file);path.parent.mkdir(parents=True,exist_ok=True);path.write_text(json.dumps(result,ensure_ascii=False,indent=2))
    print(json.dumps({'accepted':len(accepted),'rejected':len(rejected),'duplicates':len(duplicates),'file':str(path)},ensure_ascii=False),flush=True)

def apply(args):
    if not args.publish:raise RuntimeError('좌표·중복·지역 검토 후 --publish를 명시하세요.')
    data=json.loads(Path(args.file).read_text());region=int(data['regionCode']);rows=data['accepted']
    if len(rows)+len(data['rejected'])+len(data['duplicateIds'])!=data['apiTotalCount']:raise RuntimeError('입력 파일 건수 검증 실패')
    if len({r['external_id'] for r in rows})!=len(rows) or not rows:raise RuntimeError('중복 또는 빈 입력')
    for raw in data['raw']:
        row,_=normalize(raw,str(region))
        if row and row not in rows:raise RuntimeError('검토 데이터와 원본이 다릅니다.')
    helpers['load_env'](args.env_file);q=helpers['quote'];sql=helpers['sql'];query=helpers['rows'];one=helpers['one']
    if not one(f'SELECT region_id FROM region WHERE region_id={region}'):raise RuntimeError('대상 지역 코드 없음')
    version=sql('SELECT version FROM flyway_schema_history WHERE success AND version IS NOT NULL ORDER BY installed_rank DESC LIMIT 1')
    if int(version)<22:raise RuntimeError('V22 이상이 필요합니다.')
    if one("SELECT run_id FROM public_data_sync_run WHERE source_id=1701 AND status='RUNNING'"):raise RuntimeError('실행 중인 동기화와 충돌합니다.')
    before=query("SELECT * FROM public_facility WHERE source_code='KTO_PET_TOUR' ORDER BY facility_id")
    before_images=query("SELECT i.* FROM public_facility_image i JOIN public_facility f USING(facility_id) WHERE f.source_code='KTO_PET_TOUR' ORDER BY i.image_id")
    timestamp=dt.datetime.now().strftime('%Y%m%d-%H%M%S');folder=ROOT/'.demo';backup=folder/f'public-import-backup-{timestamp}.json'
    backup.write_text(json.dumps({'facilities':before,'images':before_images},ensure_ascii=False,indent=2))
    staged=json.dumps(rows,ensure_ascii=False)
    statement=f"""BEGIN;
SELECT source_id FROM public_data_source WHERE source_id=1701 FOR UPDATE;
DO $$ BEGIN IF EXISTS(SELECT 1 FROM public_data_sync_run WHERE source_id=1701 AND status='RUNNING') THEN RAISE EXCEPTION 'sync is running'; END IF; END $$;
CREATE TEMP TABLE incoming ON COMMIT DROP AS
SELECT * FROM jsonb_to_recordset({q(staged)}::jsonb) AS x(external_id TEXT,name TEXT,address TEXT,category TEXT,latitude NUMERIC,longitude NUMERIC,region_id BIGINT,phone TEXT,image_url TEXT);
INSERT INTO public_facility(source_code,external_id,name,address,category,latitude,longitude,region_id,phone,visible)
SELECT 'KTO_PET_TOUR',external_id,name,address,category,latitude,longitude,region_id,phone,TRUE FROM incoming
ON CONFLICT(source_code,external_id) DO UPDATE SET name=EXCLUDED.name,address=EXCLUDED.address,category=EXCLUDED.category,
 latitude=EXCLUDED.latitude,longitude=EXCLUDED.longitude,region_id=EXCLUDED.region_id,phone=EXCLUDED.phone,visible=TRUE
WHERE NOT public_facility.operator_corrected;
INSERT INTO public_facility_image(facility_id,image_url)
SELECT f.facility_id,i.image_url FROM incoming i JOIN public_facility f ON f.source_code='KTO_PET_TOUR' AND f.external_id=i.external_id
WHERE i.image_url IS NOT NULL AND NOT f.operator_corrected ON CONFLICT(facility_id,image_url) DO NOTHING;
COMMIT;"""
    sql(statement)
    imported=query("SELECT facility_id,external_id,name,visible,operator_corrected FROM public_facility WHERE source_code='KTO_PET_TOUR' ORDER BY facility_id")
    wanted={r['external_id'] for r in rows};matched=[r for r in imported if r['external_id'] in wanted]
    if len(matched)!=len(rows):raise RuntimeError('적재 후 원본 ID 대조 실패')
    after_by_id={r['facility_id']:r for r in query("SELECT * FROM public_facility WHERE source_code='KTO_PET_TOUR'")}
    if any(after_by_id[r['facility_id']]!=r for r in before if r['operator_corrected']):raise RuntimeError('운영자 보정 데이터 보존 검증 실패')
    report={'regionCode':region,'apiTotalCount':data['apiTotalCount'],'accepted':len(rows),'rejected':data['rejected'],'duplicateIds':data['duplicateIds'],
            'beforeCount':len(before),'afterCount':len(imported),'newCount':len(imported)-len(before),'matchedVisible':sum(r['visible'] for r in matched),
            'preservedOperatorCorrections':sum(r['operator_corrected'] for r in before),'backupFile':str(backup),
            'inputSha256':hashlib.sha256(Path(args.file).read_bytes()).hexdigest(),'places':matched}
    encoded=json.dumps(report,ensure_ascii=False,indent=2)
    (folder/f'public-import-report-{timestamp}.json').write_text(encoded)
    (folder/'seoul-public-import-report.json').write_text(encoded)
    print(json.dumps({k:v for k,v in report.items() if k!='places'},ensure_ascii=False,indent=2))

def main():
    parser=argparse.ArgumentParser(description=__doc__);parser.add_argument('action',choices=['collect','apply']);parser.add_argument('--region-code',default='11');parser.add_argument('--file',default=str(ROOT/'.demo/seoul-public-places.json'));parser.add_argument('--env-file');parser.add_argument('--publish',action='store_true');args=parser.parse_args()
    if not args.region_code.isdigit() or len(args.region_code)!=2:parser.error('법정동 시도 코드 두 자리를 사용하세요.')
    collect(args) if args.action=='collect' else apply(args)
if __name__=='__main__':
    try:main()
    except RuntimeError as e:print(str(e));raise SystemExit(1)
