#!/usr/bin/env python3
"""Explicit, resumable demo fixtures. No startup seeding; all SQL goes over stdin, never a shell.

plan is offline. inspect/verify are read-only except verify --exercise, which creates and
removes its own test dog/QR visit. apply and reset require a configured target database.
Credentials are generated once in .demo/credentials.json (0600), never printed or committed.
"""
import argparse
import datetime as dt
import hashlib
import json
import math
import os
from pathlib import Path
import secrets
import ssl
import struct
import subprocess
import sys
import urllib.error
import urllib.parse
import urllib.request
import uuid
import zlib

# macOS framework Python may lack a bundled CA file; retain TLS verification using OS roots.
if ssl.get_default_verify_paths().cafile is None and Path('/etc/ssl/cert.pem').is_file():
    os.environ.setdefault('SSL_CERT_FILE','/etc/ssl/cert.pem')

ROOT = Path(__file__).resolve().parents[1]
SEED = 'pawfit-scenario-v1'
SEOUL = dt.timezone(dt.timedelta(hours=9))
PLACES = [
    dict(name='온유커피 성수', category='CAFE', lat=37.5446, lon=127.0557, weight=7, stroller=True),
    dict(name='테라스 오브 성수', category='CAFE', lat=37.5455, lon=127.0570, weight=10, stroller=True),
    dict(name='포레스트 멍라운지', category='CAFE', lat=37.5433, lon=127.0540, weight=None, stroller=False),
    dict(name='소담펫 베이커리', category='CAFE', lat=37.5460, lon=127.0538, weight=5, stroller=False),
    dict(name='성수 아틀리에 라운지', category='CULTURE', lat=37.5425, lon=127.0565, unknown=True),
    dict(name='연무장 북갤러리', category='CULTURE', lat=37.5450, lon=127.0582, denied=True),
]
EXPECTED = [('ENTRY_OK','ENTRY_DENIED','ENTRY_DENIED'),('ENTRY_OK','PREPARE','PREPARE'),
            ('ENTRY_OK','ENTRY_OK','ENTRY_OK'),('ENTRY_OK','ENTRY_DENIED','ENTRY_DENIED'),
            ('CHECK_REQUIRED','CHECK_REQUIRED','CHECK_REQUIRED'),('ENTRY_DENIED','ENTRY_DENIED','ENTRY_DENIED')]


def quote(value):
    if value is None: return 'NULL'
    if isinstance(value, bool): return 'TRUE' if value else 'FALSE'
    if isinstance(value, (int, float)): return str(value)
    return "'" + str(value).replace("'", "''") + "'"


def load_env(path):
    if not path: return
    for line in Path(path).read_text().splitlines():
        line = line.strip()
        if line and not line.startswith('#') and '=' in line:
            key, value = line.removeprefix('export ').split('=',1)
            os.environ.setdefault(key.strip(),value.strip().strip('"').strip("'"))


def sql(statement, json_result=False):
    raw = os.environ.get('DB_URL','').removeprefix('jdbc:')
    if not raw: raise RuntimeError('DB_URL, DB_USERNAME, DB_PASSWORD를 설정해야 합니다.')
    uri = urllib.parse.urlparse(raw)
    if uri.scheme not in ('postgresql','postgres'): raise RuntimeError('PostgreSQL DB_URL이 필요합니다.')
    env = os.environ.copy()
    env.update(PGHOST=uri.hostname or 'localhost',PGPORT=str(uri.port or 5432),
               PGDATABASE=uri.path.lstrip('/'),PGUSER=os.environ.get('DB_USERNAME') or uri.username or '',
               PGPASSWORD=os.environ.get('DB_PASSWORD') or uri.password or '')
    for key,value in urllib.parse.parse_qsl(uri.query):
        if key == 'sslmode': env['PGSSLMODE'] = value
    result = subprocess.run(['psql','-X','-q','-t','-A','-v','ON_ERROR_STOP=1'],
                            input=statement,text=True,capture_output=True,env=env)
    if result.returncode: raise RuntimeError('DB 작업 실패: '+result.stderr[:1200])
    out = result.stdout.strip()
    return json.loads(out) if json_result and out else (None if json_result else out)


def rows(select):
    return sql(f"SELECT COALESCE(json_agg(q),'[]'::json) FROM ({select}) q;",True)


def one(select):
    result=rows(select)
    return result[0] if result else None


def manifest(kind, entity_id, metadata=None):
    sql(f"INSERT INTO demo_seed_manifest VALUES({quote(SEED)},{quote(kind)},{quote(entity_id)},"
        f"{quote(json.dumps(metadata,ensure_ascii=False))}) ON CONFLICT DO NOTHING;")


def claimed(kind, label):
    r=one(f"SELECT entity_id FROM demo_seed_manifest WHERE seed_key={quote(SEED)} "
          f"AND entity_type={quote(kind)} AND metadata::jsonb->>'label'={quote(label)}")
    return r['entity_id'] if r else None


class API:
    def __init__(self, base, token=None):
        self.base=base.rstrip('/').removesuffix('/api/v1')+'/api/v1'
        self.token=token
    def call(self, method,path,data=None,content=None,content_type=None):
        headers={}
        if self.token: headers['Authorization']='Bearer '+self.token
        if data is not None:
            content=json.dumps(data).encode();content_type='application/json'
        if content_type: headers['Content-Type']=content_type
        req=urllib.request.Request(self.base+path,data=content,headers=headers,method=method)
        try:
            with urllib.request.urlopen(req,timeout=90) as response:
                body=response.read()
                return json.loads(body) if body else None
        except urllib.error.HTTPError as e:
            body=e.read().decode(errors='replace')
            raise RuntimeError(f'API {method} {path}: HTTP {e.code}: {body[:700]}') from None
        except urllib.error.URLError:
            raise RuntimeError(f'API {method} {path}: 서버 연결 실패') from None
    def upload(self,path,name,content,content_type='image/png'):
        boundary='pawfit-'+uuid.uuid4().hex
        body=(f'--{boundary}\r\nContent-Disposition: form-data; name="file"; filename="{name}"\r\n'
              f'Content-Type: {content_type}\r\n\r\n').encode()+content+f'\r\n--{boundary}--\r\n'.encode()
        return self.call('POST',path,content=body,content_type='multipart/form-data; boundary='+boundary)


def credentials():
    folder=ROOT/'.demo';folder.mkdir(mode=0o700,exist_ok=True)
    path=folder/'credentials.json'
    if path.exists(): return path,json.loads(path.read_text())
    value={'seed':SEED,'accounts':{},'password':'PawFit!'+secrets.token_urlsafe(15)}
    fd=os.open(path,os.O_WRONLY|os.O_CREAT|os.O_EXCL,0o600)
    with os.fdopen(fd,'w') as f: json.dump(value,f,ensure_ascii=False,indent=2)
    return path,value


def register(api,creds_path,creds,label,phone,role,login_id=None):
    existing=one(f'SELECT user_id,name,role FROM app_user WHERE phone_number={quote(phone)}')
    owned=claimed('USER',label)
    if owned and not existing:raise RuntimeError(f'{label}: 시연 계정의 전화번호가 변경됐습니다. 기존 계정을 확인해 주세요.')
    if existing:
        if not owned or int(owned)!=existing['user_id']:
            raise RuntimeError(f'{label}: 이미 있는 계정은 덮어쓰지 않습니다. 다른 시연 번호를 지정하세요.')
        login=api.call('POST','/auth/login',{'phoneNumber':phone,'password':creds['password']})
        return existing['user_id'],API(api.base,login['accessToken'])
    verification=api.call('POST','/auth/phone-verifications',{'phoneNumber':phone,'purpose':'SIGN_UP'})
    confirmed=api.call('POST','/auth/phone-verifications/confirm',
                       {'verificationId':verification['verificationId'],'code':os.environ.get('DEMO_PHONE_CODE','123456')})
    terms=api.call('GET','/terms?role='+role)['terms']
    payload={'phoneVerificationToken':confirmed['verificationToken'],'name':'시연 '+label,
             'password':creds['password'],'termAgreements':[{'termId':t['termId'],'agreed':True} for t in terms]}
    if login_id: payload['loginId']=login_id
    result=api.call('POST','/auth/signup/'+role.lower(),payload)
    manifest('USER',result['userId'],{'label':label,'phone':phone,'role':role})
    creds['accounts'][label]={'phoneNumber':phone,'loginId':login_id,'userId':result['userId']}
    creds_path.write_text(json.dumps(creds,ensure_ascii=False,indent=2));creds_path.chmod(0o600)
    return result['userId'],API(api.base,result['accessToken'])


def fixture_png(index=0):
    """Original geometric storefront illustration, clearly marked DEMO with a bitmap font."""
    width,height=720,480
    palettes=[(106,133,105),(197,147,106),(85,118,102),(189,136,117),(126,129,164),(95,112,127)]
    color=palettes[index%len(palettes)]
    data=bytearray([243,239,229]*width*height)
    def rect(x,y,w,h,c):
        for yy in range(max(0,y),min(height,y+h)):
            for xx in range(max(0,x),min(width,x+w)):
                offset=(yy*width+xx)*3;data[offset:offset+3]=bytes(c)
    def circle(x,y,r,c):
        for yy in range(max(0,y-r),min(height,y+r+1)):
            for xx in range(max(0,x-r),min(width,x+r+1)):
                if (xx-x)**2+(yy-y)**2<=r*r: rect(xx,yy,1,1,c)
    rect(0,360,width,120,(222,215,200));rect(110,115,500,250,(235,222,201))
    rect(90,110,540,65,color);rect(150,200,170,120,(62,87,83));rect(340,200,100,165,(74,94,88))
    rect(460,200,105,120,(62,87,83));rect(158,208,154,5,(242,226,197));rect(233,200,6,120,(242,226,197))
    rect(475,208,75,5,(242,226,197));rect(370,220,45,130,(188,207,194));circle(427,284,4,(234,203,135))
    for x in [80,640]:
        rect(x-5,285,10,85,(133,99,74));circle(x,265,34,color);circle(x-18,280,25,color);circle(x+18,280,25,color)
        rect(x-24,330,48,40,(171,132,105))
    font={'D':['1110','1001','1001','1001','1110'],'E':['1111','1000','1110','1000','1111'],
          'M':['10001','11011','10101','10001','10001'],'O':['111','101','101','101','111']}
    x=260
    for letter in 'DEMO':
        glyph=font[letter]
        for yy,line in enumerate(glyph):
            for xx,c in enumerate(line):
                if c=='1': rect(x+xx*8,122+yy*8,7,7,(250,246,235))
        x+=(len(glyph[0])+2)*8
    def chunk(name,body): return struct.pack('!I',len(body))+name+body+struct.pack('!I',zlib.crc32(name+body)&0xffffffff)
    scan=b''.join(b'\0'+bytes(data[y*width*3:(y+1)*width*3]) for y in range(height))
    return b'\x89PNG\r\n\x1a\n'+chunk(b'IHDR',struct.pack('!2I5B',width,height,8,2,0,0,0))+chunk(b'IDAT',zlib.compress(scan))+chunk(b'IEND',b'')


def conditions(place):
    return {'petsAllowed':not place.get('denied',False),
            'indoor':{'allowed':True,'maxWeightKg':place.get('weight'),
                      'equipmentCodes':['STROLLER'] if place.get('stroller') else []},
            'outdoor':{'allowed':False,'maxWeightKg':None,'equipmentCodes':['LEASH']},
            'effectiveFrom':(dt.datetime.now(SEOUL).date()-dt.timedelta(days=30)).isoformat(),
            'notice':'시연용 가상 장소의 입장 조건입니다.','changeReason':'시연 초기 정책','vaccinationConfirmed':False}


def create_store(api,business_id,place,index,doc_url):
    known=claimed('STORE',place['name'])
    if known:return int(known)
    result=api.call('POST','/business/verifications',{'businessNumber':str(9900000000+index),
        'businessName':place['name'],'representativeName':'시연 담당자','documentUrl':doc_url,
        'store':{'name':place['name'],'address':f'서울특별시 성동구 성수이로 시연구역 {index+1}',
                 'phone':'02-0000-'+str(1000+index),'latitude':place['lat'],'longitude':place['lon']}})
    verification_id=result['verificationId']
    store=one(f'SELECT store_id FROM store_verification WHERE verification_id={verification_id}')['store_id']
    sql(f"BEGIN; UPDATE store_verification SET status='APPROVED',review_note='시연용 가상 업체 승인',reviewed_at=now() "
        f"WHERE verification_id={verification_id}; UPDATE store SET verification_status='VERIFIED',visibility='VISIBLE',"
        f"region_id=11,category={quote(place['category'])},description='시연용 가상 장소입니다. 실제 업체 정보가 아닙니다.' WHERE store_id={store};"
        f"INSERT INTO store_manager(store_id,user_id) VALUES({store},{business_id}); COMMIT;")
    manifest('STORE',store,{'label':place['name'],'index':index})
    manifest('VERIFICATION',verification_id,{'label':place['name']})
    return store


def insert_returning(table,values,key):
    return int(sql(f"INSERT INTO {table}({','.join(values)}) VALUES({','.join(quote(v) for v in values.values())}) RETURNING {key};"))


def historical(table,kind,label,values,key):
    known=claimed(kind,label)
    if known:return int(known)
    ident=insert_returning(table,values,key);manifest(kind,ident,{'label':label,'values':values});return ident


def seed_public(api):
    nearby=rows("SELECT facility_id,name FROM public_facility WHERE visible=TRUE "
                "AND latitude BETWEEN 37.49 AND 37.60 AND longitude BETWEEN 126.99 AND 127.12 ORDER BY facility_id LIMIT 2")
    if len(nearby)>=2:
        for r in nearby:
            if not claimed('PUBLIC_CREATED',r['name']):manifest('PUBLIC_REUSED',r['facility_id'],{'label':r['name']})
        return
    key=os.environ.get('DATA_GO_KR_SERVICE_KEY')
    local=ROOT/'.env'
    if not key and local.exists():
        for line in local.read_text().splitlines():
            if line.startswith('DATA_GO_KR_SERVICE_KEY='):key=line.split('=',1)[1].strip().strip('"').strip("'")
    if not key:raise RuntimeError('공공데이터 2곳을 준비하려면 DATA_GO_KR_SERVICE_KEY가 필요합니다.')
    collected=[]
    for page in range(1,21):
        query=urllib.parse.urlencode({'serviceKey':key,'MobileOS':'ETC','MobileApp':'PawFit','_type':'json',
                                     'areaCode':1,'numOfRows':100,'pageNo':page,'arrange':'A'})
        try:
            with urllib.request.urlopen('https://apis.data.go.kr/B551011/KorPetTourService2/areaBasedList2?'+query,timeout=30) as response:
                response_data=json.load(response)['response']
                if response_data.get('header',{}).get('resultCode') not in ['0000','00']:raise RuntimeError('공공데이터 API 결과 오류')
                body=response_data['body']
        except Exception:raise RuntimeError('공공데이터 수집 실패 (인증키와 원본 URL은 출력하지 않습니다).') from None
        items=body.get('items',{}).get('item',[]) if isinstance(body.get('items'),dict) else []
        if isinstance(items,dict):items=[items]
        for r in items:
            try:
                lat,lon=float(r['mapy']),float(r['mapx'])
                distance=6371000*2*math.asin(math.sqrt(math.sin(math.radians(lat-37.5446)/2)**2+math.cos(math.radians(lat))*math.cos(math.radians(37.5446))*math.sin(math.radians(lon-127.0557)/2)**2))
                if distance<=5000:collected.append(r)
            except (KeyError,ValueError,TypeError):pass
        if page*100>=int(body.get('totalCount',0)):break
    collected.sort(key=lambda r:(float(r['mapy'])-37.5446)**2+(float(r['mapx'])-127.0557)**2)
    for r in collected:
        ext=str(r['contentid']);known=one(f"SELECT facility_id,visible,operator_corrected FROM public_facility WHERE source_code='KTO_PET_TOUR' AND external_id={quote(ext)}")
        if known and known['operator_corrected'] and not known['visible']:continue
        if known:
            ident=known['facility_id']
            manifest('PUBLIC_REUSED',ident,{'label':r['title'],'originalVisible':known['visible']})
            sql(f'UPDATE public_facility SET visible=TRUE WHERE facility_id={ident}')
        else:
            ident=insert_returning('public_facility',{'source_code':'KTO_PET_TOUR','external_id':ext,'name':r['title'][:150],
                'address':str(r.get('addr1',''))[:255],'category':{'12':'ATTRACTION','14':'CULTURE','15':'EVENT','28':'LEISURE','32':'LODGING','38':'SHOPPING','39':'FOOD'}.get(str(r.get('contenttypeid')),'ATTRACTION'),
                'latitude':float(r['mapy']),'longitude':float(r['mapx']),'region_id':11,'visible':True,'phone':str(r.get('tel',''))[:80]},'facility_id')
            manifest('PUBLIC_CREATED',ident,{'label':r['title']})
        image=r.get('firstimage') or r.get('firstimage2')
        if image and image.startswith('http://tong.visitkorea.or.kr/'):image=image.replace('http://','https://',1)
        if image and image.startswith(('https://','http://')):
            sql(f"INSERT INTO public_facility_image(facility_id,image_url) SELECT {ident},{quote(image)} WHERE NOT EXISTS "
                f"(SELECT 1 FROM public_facility_image WHERE facility_id={ident} AND image_url={quote(image)});")
        nearby.append({'facility_id':ident})
        if len({p['facility_id'] for p in nearby})>=2:return
    raise RuntimeError('시연 위치 주변 공공데이터 장소가 2곳 미만입니다. 실제 원본을 확인해 주세요.')


def apply(args):
    report=inspect_db()
    if report['version']!='22':raise RuntimeError('V22 배포와 마이그레이션 적용을 먼저 완료해 주세요.')
    path,creds=credentials();api=API(args.api_base)
    guardian_id,guardian=register(api,path,creds,'보호자','01099000001','GUARDIAN')
    business_id,business=register(api,path,creds,'업체 담당자','01099000002','BUSINESS','owner.demo@pawfit.example')
    users=[]
    for i in range(args.aux_count):
        user_id,_=register(api,path,creds,f'통계 보호자 {i+1:02d}',str(1099000100+i).zfill(11),'GUARDIAN');users.append(user_id)
    assets_dir=ROOT/'.demo'/'assets';assets_dir.mkdir(exist_ok=True)
    doc_id=claimed('ASSET','evidence')
    if not doc_id:
        uploaded=business.upload('/files','demo-evidence.png',fixture_png(4));doc_id=uploaded['assetId']
        manifest('ASSET',doc_id,{'label':'evidence'})
    doc_url=api.base.removesuffix('/api/v1')+'/api/v1/files/'+doc_id
    store_ids=[None]*len(PLACES)
    for i in reversed(range(len(PLACES))):store_ids[i]=create_store(business,business_id,PLACES[i],i,doc_url)
    for i,(place,store_id) in enumerate(zip(PLACES,store_ids)):
        if not place.get('unknown') and not claimed('POLICY',place['name']):
            value=conditions(place)
            policy_id=historical('store_policy','POLICY',place['name'],{'store_id':store_id,'version':1,
                'source_text':f"{place['name']} 시연용 정책. "+('반려견 동반 불가' if place.get('denied') else f"실내 {place.get('weight') or '제한 없음'}kg, 유모차 {'필수' if place.get('stroller') else '선택'}"),
                'conditions_json':json.dumps(value,ensure_ascii=False),'created_at':(dt.datetime.now(SEOUL)-dt.timedelta(days=10)).isoformat()},'policy_id')
            if place.get('stroller'):sql(f"INSERT INTO store_policy_rule VALUES({policy_id},'EQUIPMENT','STROLLER',TRUE)")
        business.call('PATCH',f'/business/stores/{store_id}/business-hours',{'hours':[
            {'dayOfWeek':day,'openTime':'10:00','closeTime':'20:00','closed':False}
            for day in ['MONDAY','TUESDAY','WEDNESDAY','THURSDAY','FRIDAY','SATURDAY','SUNDAY']]})
        if not claimed('IMAGE',place['name']):
            content=fixture_png(i);(assets_dir/f'store-{i+1}.png').write_bytes(content)
            uploaded=business.upload(f'/business/stores/{store_id}/images',f'store-{i+1}.png',content)
            manifest('IMAGE',uploaded['assetId'],{'label':place['name']})
    now=dt.datetime.now(SEOUL);today=now.date()
    current_id=historical('stamp_event','EVENT','current',{'title':'성수 가을 스탬프 투어','event_year':today.year,
        'quarter':(today.month-1)//3+1,'status':'OPEN','starts_on':(today-dt.timedelta(days=45)).isoformat(),
        'ends_on':(today+dt.timedelta(days=45)).isoformat(),'required_stamp_count':2,'max_stamp_count':6,
        'notice':'시연용 이벤트. 동일 장소는 한 번만 적립됩니다.'},'event_id')
    winter_start=today+dt.timedelta(days=50)
    winter_id=historical('stamp_event','EVENT','winter',{'title':'서울숲 겨울 산책 스탬프','event_year':winter_start.year,
        'quarter':(winter_start.month-1)//3+1,'status':'OPEN','starts_on':winter_start.isoformat(),
        'ends_on':(winter_start+dt.timedelta(days=60)).isoformat(),'required_stamp_count':2,'max_stamp_count':6,
        'recruitment_starts_on':(today-dt.timedelta(days=7)).isoformat(),'recruitment_ends_on':(winter_start-dt.timedelta(days=1)).isoformat(),
        'notice':'겨울 산책 이벤트. 현재는 업체 사전 모집 기간입니다.'},'event_id')
    for event_id in [current_id,winter_id]:
        sql(f'INSERT INTO event_region VALUES({event_id},11) ON CONFLICT DO NOTHING;')
        if not one(f'SELECT prize_id FROM event_prize WHERE event_id={event_id}'):
            historical('event_prize','PRIZE',str(event_id),{'event_id':event_id,'prize_rank':1,'name':'반려견 산책 키트','quantity':3},'prize_id')
    for i,store_id in enumerate(store_ids):
        if not claimed('QR',str(store_id)):
            business.call('POST',f'/business/stores/{store_id}/qr',{'reason':'시연용 최초 발급'})
            qr=one(f'SELECT qr_id FROM store_qr WHERE store_id={store_id} ORDER BY qr_id DESC LIMIT 1')
            manifest('QR',qr['qr_id'],{'label':str(store_id)})
        link=one(f'SELECT event_id FROM event_store WHERE store_id={store_id}')
        if link['event_id']!=current_id:raise RuntimeError('QR이 현재 시연 이벤트에 연결되지 않았습니다. 기존 이벤트와의 충돌을 확인하세요.')
    dog_id=claimed('DOG','몽이')
    if not dog_id:
        breed=next(b for b in guardian.call('GET','/breeds')['breeds'] if b['name']=='말티즈')
        dog=guardian.call('POST','/dogs',{'name':'몽이','breedId':breed['breedId'],'birthDate':'2022-03-14',
             'weightKg':5,'equipmentCodes':['STROLLER','CARRIER','BELLY_BAND','LEASH']})
        dog_id=dog['dogId'];manifest('DOG',dog_id,{'label':'몽이'})
    if not one(f'SELECT record_id FROM vaccination_record WHERE dog_id={dog_id}'):
        record=historical('vaccination_record','VACCINATION','몽이',{'dog_id':int(dog_id),'status':'VERIFIED','revision':1},'record_id')
        for code in ['RABIES','DHPPL']:
            historical('vaccination_item','VACCINE',code,{'record_id':record,'source':'OPERATOR','vaccine_code':code,
                'vaccinated_at':(today-dt.timedelta(days=60)).isoformat(),'confidence':1},'item_id')
        sql(f"UPDATE dog SET vaccination_status='VERIFIED' WHERE dog_id={dog_id}")
    main=store_ids[0]
    old_time=(now-dt.timedelta(days=100)).isoformat()
    sql(f"UPDATE app_user SET created_at={quote(old_time)} WHERE user_id IN ({','.join(map(str,[guardian_id,business_id,*users]))});"
        f"UPDATE store SET created_at={quote(old_time)} WHERE store_id IN ({','.join(map(str,store_ids))});"
        f"UPDATE store_qr SET created_at={quote(old_time)} WHERE store_id IN ({','.join(map(str,store_ids))});")
    for i,user_id in enumerate(users):
        days=[i%7+1,i%23+8,i%59+31][i%3]
        at=dt.datetime.combine(today-dt.timedelta(days=days),dt.time(10+i%10),SEOUL).isoformat()
        qr=one(f'SELECT qr_token_hash FROM event_store WHERE store_id={main}')['qr_token_hash']
        historical('visit','VISIT',str(user_id),{'user_id':user_id,'store_id':main,'qr_token_hash':qr,'visited_at':at},'visit_id')
        if days<=45:historical('event_stamp','STAMP',str(user_id),{'event_id':current_id,'user_id':user_id,'store_id':main,'awarded_at':at},'stamp_id')
    # Main guardian has one stamp elsewhere; scanning main store visibly increases count from 1 to 2.
    other=store_ids[2];other_qr=one(f'SELECT qr_token_hash FROM event_store WHERE store_id={other}')['qr_token_hash']
    at=(now-dt.timedelta(days=2)).isoformat()
    historical('visit','VISIT','guardian-initial',{'user_id':guardian_id,'store_id':other,'qr_token_hash':other_qr,'visited_at':at},'visit_id')
    historical('event_stamp','STAMP','guardian-initial',{'event_id':current_id,'user_id':guardian_id,'store_id':other,'awarded_at':at},'stamp_id')
    for status,label in [('RECEIVED','안내 문구 변경'),('NEEDS_SUPPLEMENT','예방접종 조건 보완'),('APPROVED','준비물 안내 승인'),('REJECTED','미확인 조건 반려')]:
        historical('condition_request','CONDITION',label,{'store_id':main,'base_version':0 if status=='APPROVED' else 1,'source_text':label,
            'submitted_by':business_id,'status':status,'kind':'CONDITION_REQUEST','conditions_json':json.dumps(conditions(PLACES[0]),ensure_ascii=False),
            'review_note':'근거 자료의 적용 날짜가 보이지 않습니다.' if status=='NEEDS_SUPPLEMENT' else ('시연용 심사 결과' if status in ['APPROVED','REJECTED'] else None),
            'reviewed_at':(now-dt.timedelta(days=10 if status=='APPROVED' else 1)).isoformat() if status!='RECEIVED' else None,
            'created_at':(now-dt.timedelta(days=15 if status=='APPROVED' else 3)).isoformat()},'request_id')
    approved=claimed('CONDITION','준비물 안내 승인')
    sql(f"UPDATE store_policy SET request_id={approved},changed_by='OPERATOR' WHERE policy_id={claimed('POLICY',PLACES[0]['name'])};")
    for row in rows(f"SELECT entity_id FROM demo_seed_manifest WHERE seed_key={quote(SEED)} AND entity_type='CONDITION'"):
        request_id=int(row['entity_id'])
        sql(f"INSERT INTO condition_request_rule VALUES({request_id},'EQUIPMENT','STROLLER',TRUE) ON CONFLICT DO NOTHING;")
    supplement=claimed('CONDITION','예방접종 조건 보완')
    sql(f"INSERT INTO condition_request_rule VALUES({supplement},'VACCINE','RABIES',TRUE) ON CONFLICT DO NOTHING;")
    sql(f'INSERT INTO condition_request_evidence SELECT {supplement},{quote(doc_url)} WHERE NOT EXISTS(SELECT 1 FROM condition_request_evidence WHERE request_id={supplement});')
    historical('event_application','EVENT_APPLICATION','autumn-approved',{'store_id':main,'event_id':current_id,
        'submitted_by':business_id,'message':'성수 가을 행사 참여','status':'APPROVED','review_note':'시연용 참여 승인',
        'reviewed_at':(now-dt.timedelta(days=30)).isoformat(),'created_at':(now-dt.timedelta(days=35)).isoformat()},'application_id')
    autumn_app=claimed('EVENT_APPLICATION','autumn-approved')
    sql(f'INSERT INTO event_application_region VALUES({autumn_app},11) ON CONFLICT DO NOTHING;')
    for status in ['APPROVED','REJECTED']:
        historical('ad_application','AD',status,{'store_id':main,'submitted_by':business_id,'ad_type':'RECOMMENDED',
            'start_date':(today-dt.timedelta(days=21)).isoformat(),'end_date':(today-dt.timedelta(days=8)).isoformat(),
            'creative_url':doc_url,'message':'시연용 지도 추천 광고','status':status,'review_note':'자료 재확인 필요' if status=='REJECTED' else '승인',
            'reviewed_at':(now-dt.timedelta(days=25)).isoformat(),'created_at':(now-dt.timedelta(days=26)).isoformat()},'application_id')
    for status in ['WAITING','ANSWERED']:
        historical('inquiry','INQUIRY',status,{'user_id':business_id,'category':'EVENT','title':'이벤트 참여 일정 문의' if status=='WAITING' else 'QR 인증 수 차이 문의',
            'content':'시연용 문의입니다. 이벤트 및 QR 인증 집계 기준을 안내해 주세요.','status':status,
            'answer':'QR 인증 수와 스탬프 적립 수는 조건이 달라 차이가 날 수 있습니다.' if status=='ANSWERED' else None,
            'answered_at':(now-dt.timedelta(days=1)).isoformat() if status=='ANSWERED' else None,
            'created_at':(now-dt.timedelta(days=4)).isoformat()},'inquiry_id')
    seed_public(api)
    manifest('RUN','complete',{'label':'complete','seededAt':now.isoformat()})
    exported=rows(f"SELECT entity_type,entity_id,metadata FROM demo_seed_manifest WHERE seed_key={quote(SEED)} ORDER BY entity_type,entity_id")
    (ROOT/'.demo'/'manifest.json').write_text(json.dumps(exported,ensure_ascii=False,indent=2))
    print('시연 데이터 준비 완료. 계정 정보:',path)
    verify(args)


def inspect_db():
    version=sql("SELECT version FROM flyway_schema_history WHERE success=TRUE AND version IS NOT NULL ORDER BY installed_rank DESC LIMIT 1;")
    result={'version':version,'users':one('SELECT count(*) AS count FROM app_user')['count'],
            'stores':one('SELECT count(*) AS count FROM store')['count'],
            'publicPlaces':one('SELECT count(*) AS count FROM public_facility')['count'],
            'events':rows('SELECT event_id,title,status,starts_on,ends_on FROM stamp_event ORDER BY event_id')}
    print(json.dumps(result,ensure_ascii=False,indent=2));return result


def verify(args):
    path,creds=credentials();api=API(args.api_base)
    login=api.call('POST','/auth/login',{'phoneNumber':'01099000001','password':creds['password']})
    guardian=API(api.base,login['accessToken']);dog_id=int(claimed('DOG','몽이'))
    dog_list=guardian.call('GET','/dogs')['dogs']
    if len(dog_list)!=1 or dog_list[0]['name']!='몽이':raise RuntimeError('초기 반려견 상태가 몽이 한 마리가 아닙니다. reset을 사용하세요.')
    for i,place in enumerate(PLACES):
        store_id=claimed('STORE',place['name']);detail=guardian.call('GET',f'/places/STORE:{store_id}?dogIds={dog_id}')
        assert detail['verdict']==EXPECTED[i][0],(place['name'],detail)
        assert detail.get('category')==place['category']
        assert detail.get('imageUrls'),place['name']+' 이미지 누락'
        with urllib.request.urlopen(detail['imageUrls'][0],timeout=30) as image:assert image.status==200
    if args.exercise:
        breed=next(b for b in guardian.call('GET','/breeds')['breeds'] if b['name']=='믹스견')
        kong=guardian.call('POST','/dogs',{'name':'콩이','breedId':breed['breedId'],'birthDate':'2023-05-10','weightKg':8,'equipmentCodes':['BELLY_BAND','LEASH']})['dogId']
        try:
            for i,place in enumerate(PLACES):
                store_id=claimed('STORE',place['name'])
                for dog_ids,expected in [(str(kong),EXPECTED[i][1]),(f'{dog_id}&dogIds={kong}',EXPECTED[i][2])]:
                    actual=guardian.call('GET',f'/places/STORE:{store_id}?dogIds={dog_ids}')['verdict'];assert actual==expected,(place['name'],actual,expected)
            session=guardian.call('POST','/chat/sessions')['sessionId']
            proposal=guardian.call('POST',f'/chat/sessions/{session}/messages',{'content':'콩이 프로필에 유모차 추가'})['assistantMessage']['profileAction']
            guardian.call('POST',f"/chat/sessions/{session}/profile-actions/{proposal['actionId']}/confirm")
            assert 'STROLLER' in guardian.call('GET',f'/dogs/{kong}')['equipmentCodes']
            guardian.call('DELETE',f'/chat/sessions/{session}')
            main=claimed('STORE',PLACES[0]['name'])
            token=one(f'SELECT qr_token FROM store_qr WHERE store_id={main} ORDER BY qr_id DESC LIMIT 1')['qr_token']
            event_id=int(claimed('EVENT','current'));before=guardian.call('GET',f'/events/{event_id}/stamps')['stampCount']
            result=guardian.call('POST','/visits/qr',{'qrToken':token,'dogIds':[dog_id,kong]})
            try:assert result['stampAwarded'] and result['stampCount']==before+1
            finally:
                sql(f"BEGIN; DELETE FROM visit_dog WHERE visit_id={result['visitId']}; DELETE FROM visit WHERE visit_id={result['visitId']}; "
                    f"DELETE FROM event_stamp WHERE event_id={event_id} AND user_id={login['user']['userId']} AND store_id={main}; COMMIT;")
        finally:guardian.call('DELETE',f'/dogs/{kong}')
    business=api.call('POST','/auth/login',{'loginId':'owner.demo@pawfit.example','password':creds['password']})
    partner=API(api.base,business['accessToken']);main=claimed('STORE',PLACES[0]['name'])
    assert partner.call('GET',f'/business/events/{claimed("EVENT","winter")}/eligibility?placeId={main}')['canApply']
    assert partner.call('GET',f'/business/stores/{main}/visit-stats?from={(dt.datetime.now(SEOUL).date()-dt.timedelta(days=90)).isoformat()}&to={dt.datetime.now(SEOUL).date()}')['totalVisits']>0
    assert len(partner.call('GET','/business/faqs')['faqs'])>=3
    assert partner.call('GET',f'/business/stores/{main}/dashboard')['inProgressRequestCount']>=1
    print('검증 완료: 몽이 초기 상태·장소 판정·이미지·업체 로그인·통계·FAQ·겨울 사전 신청'+('·콩이/함께 판정·챗봇 수정·QR 적립' if args.exercise else ''))


def reset(args):
    if not args.confirm_reset:raise RuntimeError('시연 계정의 시연 중 작업을 초기화하려면 --confirm-reset이 필요합니다.')
    path,creds=credentials();api=API(args.api_base)
    guardian_id=int(claimed('USER','보호자'));business_id=int(claimed('USER','업체 담당자'))
    stores=[int(r['entity_id']) for r in rows(f"SELECT entity_id FROM demo_seed_manifest WHERE seed_key={quote(SEED)} AND entity_type='STORE'")]
    if len(stores)!=6:raise RuntimeError('시연 업체 manifest가 완전하지 않습니다. 초기화를 중단합니다.')
    owned=','.join(map(str,stores));seed=quote(SEED)
    backup=rows(f'SELECT dog_id,name,weight_kg,is_default FROM dog WHERE user_id={guardian_id}')
    backup_path=ROOT/'.demo'/('reset-backup-'+dt.datetime.now().strftime('%Y%m%d-%H%M%S')+'.json')
    snapshot={'dogs':backup}
    for table,where in [('condition_request',f'submitted_by={business_id} AND store_id IN ({owned})'),
                        ('inquiry',f'user_id={business_id}'),('ad_application',f'submitted_by={business_id}'),
                        ('event_application',f'submitted_by={business_id}'),('visit',f'user_id={guardian_id}'),
                        ('event_stamp',f'user_id={guardian_id}')]:snapshot[table]=rows(f'SELECT * FROM {table} WHERE {where}')
    backup_path.write_text(json.dumps(snapshot,ensure_ascii=False,indent=2));backup_path.chmod(0o600)
    login=api.call('POST','/auth/login',{'phoneNumber':'01099000001','password':creds['password']});guardian=API(api.base,login['accessToken'])
    mong=int(claimed('DOG','몽이'))
    for dog in guardian.call('GET','/dogs')['dogs']:
        if dog['dogId']!=mong:guardian.call('DELETE',f"/dogs/{dog['dogId']}")
    detail=guardian.call('GET',f'/dogs/{mong}')
    guardian.call('PATCH',f'/dogs/{mong}',{'name':'몽이','breedId':detail['breedId'],'weightKg':5,'equipmentCodes':['STROLLER','CARRIER','BELLY_BAND','LEASH']})
    statements=['BEGIN']
    initial_policies=f"SELECT entity_id::bigint FROM demo_seed_manifest WHERE seed_key={seed} AND entity_type='POLICY'"
    extra_policies=f'SELECT policy_id FROM store_policy WHERE store_id IN ({owned}) AND policy_id NOT IN ({initial_policies})'
    statements.append(f'DELETE FROM store_policy_rule WHERE policy_id IN ({extra_policies})')
    statements.append(f'DELETE FROM store_policy WHERE policy_id IN ({extra_policies})')
    # Restrict deletion to manifest-owned demo accounts and stores; original historical rows survive.
    for table,kind,key,where in [('inquiry','INQUIRY','inquiry_id',f'user_id={business_id}'),
        ('condition_request','CONDITION','request_id',f'submitted_by={business_id} AND store_id IN ({owned})'),
        ('ad_application','AD','application_id',f'submitted_by={business_id} AND store_id IN ({owned})'),
        ('event_application','EVENT_APPLICATION','application_id',f'submitted_by={business_id} AND store_id IN ({owned})'),
        ('visit','VISIT','visit_id',f'user_id={guardian_id} AND store_id IN ({owned})'),
        ('event_stamp','STAMP','stamp_id',f'user_id={guardian_id} AND store_id IN ({owned})')]:
        ids=f"SELECT entity_id::bigint FROM demo_seed_manifest WHERE seed_key={seed} AND entity_type={quote(kind)}"
        target=f'SELECT {key} FROM {table} WHERE {where} AND {key} NOT IN ({ids})'
        for child in {'condition_request':['condition_request_rule','condition_request_evidence'],
                      'inquiry':['inquiry_attachment','inquiry_answer_attachment'], 'event_application':['event_application_region'],
                      'visit':['visit_dog']}.get(table,[]):statements.append(f'DELETE FROM {child} WHERE {key} IN ({target})')
        statements.append(f'DELETE FROM {table} WHERE {where} AND {key} NOT IN ({ids})')
    statements.append(f'DELETE FROM draw_entry WHERE user_id={guardian_id} AND event_id={claimed("EVENT","current")}')
    for r in rows(f"SELECT entity_id,metadata FROM demo_seed_manifest WHERE seed_key={seed} AND entity_type='CONDITION'"):
        value=json.loads(r['metadata'])['values'];statements.append('UPDATE condition_request SET '+','.join(k+'='+quote(v) for k,v in value.items())+f" WHERE request_id={r['entity_id']}")
    original_requests=f"SELECT entity_id::bigint FROM demo_seed_manifest WHERE seed_key={seed} AND entity_type='CONDITION'"
    statements.append(f'DELETE FROM condition_request_rule WHERE request_id IN ({original_requests})')
    statements.append(f"INSERT INTO condition_request_rule SELECT entity_id::bigint,'EQUIPMENT','STROLLER',TRUE FROM demo_seed_manifest WHERE seed_key={seed} AND entity_type='CONDITION'")
    statements.append(f"INSERT INTO condition_request_rule VALUES({claimed('CONDITION','예방접종 조건 보완')},'VACCINE','RABIES',TRUE)")
    statements.append(f'DELETE FROM condition_request_evidence WHERE request_id IN ({original_requests})')
    evidence_url=api.base.removesuffix('/api/v1')+'/api/v1/files/'+claimed('ASSET','evidence')
    statements.append(f"INSERT INTO condition_request_evidence VALUES({claimed('CONDITION','예방접종 조건 보완')},{quote(evidence_url)})")
    statements+=['COMMIT'];sql(';'.join(statements)+';')
    print('시연 작업 초기화 완료. 백업:',backup_path)
    verify(args)


def main():
    p=argparse.ArgumentParser(description=__doc__);p.add_argument('action',choices=['plan','inspect','apply','verify','reset'])
    p.add_argument('--env-file');p.add_argument('--api-base',default='https://pawfit-api.onrender.com')
    p.add_argument('--aux-count',type=int,default=30);p.add_argument('--exercise',action='store_true')
    p.add_argument('--confirm-reset',action='store_true');args=p.parse_args();load_env(args.env_file)
    if args.action=='plan':
        print(json.dumps({'seed':SEED,'places':PLACES,'initialDog':'몽이 only','kong':'8kg, BELLY_BAND + LEASH; create during demo',
                          'auxGuardians':args.aux_count,'matrix':EXPECTED},ensure_ascii=False,indent=2));return
    if args.action=='inspect':inspect_db()
    elif args.action=='apply':apply(args)
    elif args.action=='verify':verify(args)
    else:reset(args)

if __name__=='__main__':
    try: main()
    except (RuntimeError,AssertionError) as e:
        print('실행 중단:',str(e),file=sys.stderr);sys.exit(1)
