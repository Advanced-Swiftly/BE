# 시나리오 시연 데이터와 API

이 문서는 사용자 앱·업체 웹의 시연 초기 상태와 재시연 방법을 정의합니다.
더미 데이터는 애플리케이션 시작 시 생성하지 않습니다. `scripts/demo_seed.py`를 명시적으로 실행합니다.
모든 시연 업체는 가상의 장소이며 소개에 시연용임을 표시합니다. 대표 이미지는 직접 생성한
DEMO 표시 일러스트입니다. 공공데이터 장소는 원본 명칭과 좌표를 유지합니다.

## 초기 상태

- 보호자: `GUARDIAN`, 몽이만 등록. 몽이는 말티즈·5kg·유모차/이동장/매너벨트/리드줄 보유,
  예방접종은 운영자 확인 완료 이력입니다.
- 콩이: 시연 중 직접 등록. 믹스견·8kg·매너벨트/리드줄만 보유하도록 입력합니다.
- 업체: 이메일 로그인 가능한 `BUSINESS` 계정이 여섯 가상 업체를 관리합니다.
  업체 선택 목록의 첫 업체는 온유커피 성수입니다.
- 통계: 별도의 보호자 30명, 최근 90일 방문 30건. 해당 이벤트 기간 안의 방문만 스탬프를 가집니다.
- 주 보호자는 포레스트 멍라운지 방문·스탬프 1개를 가지고 시작합니다. 온유커피 QR은 미인증입니다.
- 현재 행사: 성수 가을 스탬프 투어. 겨울 행사는 실제 시작일과 별도로 사전 모집 기간을 가집니다.
- 업체 이력: 입장 조건 접수/보완/승인/반려 각 1건, 가을 행사 참여 승인, 광고 승인/반려 각 1건,
  답변 대기/완료 문의 각 1건, FAQ 3개입니다.
- 새 체중 변경 요청·겨울 행사 신청·추천 광고 신청·광고 일정 문의는 시연 중 생성합니다.

| 장소 | 초기 조건 | 몽이 | 콩이 | 함께 |
| --- | --- | --- | --- | --- |
| 온유커피 성수 | 실내 7kg 이하, 유모차 | ENTRY_OK | ENTRY_DENIED | ENTRY_DENIED |
| 테라스 오브 성수 | 실내 10kg 이하, 유모차 | ENTRY_OK | PREPARE | PREPARE |
| 포레스트 멍라운지 | 제한 없음 | ENTRY_OK | ENTRY_OK | ENTRY_OK |
| 소담펫 베이커리 | 5kg 이하 | ENTRY_OK | ENTRY_DENIED | ENTRY_DENIED |
| 성수 아틀리에 라운지 | 정책 미확인 | CHECK_REQUIRED | CHECK_REQUIRED | CHECK_REQUIRED |
| 연무장 북갤러리 | 반려견 동반 불가 | ENTRY_DENIED | ENTRY_DENIED | ENTRY_DENIED |

초기 온유커피의 야외 구역은 이용 불가로 둡니다. 이를 허용하면 8kg 반려견도 야외를 이용할 수 있어
위 판정표의 '불가'와 충돌하기 때문입니다. 변경안에서 실내 10kg·야외 리드줄 조건을 선택할 수 있습니다.
공공데이터는 시연 좌표 37.5446, 127.0557 반경 5km에서 실제 원본 2곳을 고릅니다.
현재 원본에는 그루브하우스와 DDP가 해당합니다. 세부 정책은 추정하지 않습니다.

## 실행

Python 3, `psql`, 배포된 V22와 정상적인 API·Storage 설정이 필요합니다.
DB 접속값은 쉘 기록이나 저장소에 넣지 않고 권한이 제한된 임시 환경 파일 또는 프로세스 환경으로 전달합니다.
`DATA_GO_KR_SERVICE_KEY`는 환경에서 읽거나 기존 로컬 `.env`의 해당 키만 읽습니다.

```sh
python3 scripts/demo_seed.py plan
python3 scripts/demo_seed.py inspect --env-file /private/tmp/pawfit-db.env
python3 scripts/demo_seed.py apply --env-file /private/tmp/pawfit-db.env
python3 scripts/demo_seed.py verify --env-file /private/tmp/pawfit-db.env --exercise
python3 scripts/demo_seed.py reset --env-file /private/tmp/pawfit-db.env --confirm-reset
```

환경 파일의 키: `DB_URL`(JDBC PostgreSQL URL), `DB_USERNAME`, `DB_PASSWORD`.
기본 API는 `https://pawfit-api.onrender.com`이며 `--api-base`로 대상 API를 지정할 수 있습니다.
고정 인증번호가 켜진 시연 서버에서 실행하며 기본 번호는 123456입니다.
다른 번호는 `DEMO_PHONE_CODE`로 지정합니다. 실제 SMS 서버에서는 이 시드를 실행하지 않습니다.

`apply`는 API로 가입·반려견 등록·업체 인증 접수·사진 업로드·영업시간 설정·QR 발급을 진행합니다.
과거 승인·신청·문의·방문·스탬프는 SQL로 넣고 외래키와 실제 정책/QR 관계를 맞춥니다.
`demo_seed_manifest`에 생성한 식별자를 저장하므로 재실행해도 같은 데이터를 중복 생성하지 않습니다.
시연 중 변경된 상태를 자동으로 덮어쓰지 않으며 초기 상태가 다르면 `reset`을 안내합니다.
시연 계정 번호와 기존 계정이 충돌하면 중단합니다.

생성물은 Git에서 제외된 `.demo/`에 저장됩니다.

- `credentials.json`: 생성한 테스트 비밀번호·이메일·전화번호·사용자 ID. 파일 권한 0600.
- `manifest.json`: 생성한 데이터의 종류·ID·초기 메타데이터.
- `assets/`: 직접 생성한 가상 매장 이미지.
- `reset-backup-*.json`: 초기화 직전 시연 계정 데이터 백업.

`verify --exercise`는 콩이를 잠시 생성해 세 가지 선택의 판정표, 챗봇 확인 후 준비물 추가,
QR 적립을 검증하고 본인이 만든 임시 반려견·방문·스탬프를 제거합니다.
`reset`은 manifest로 식별한 시연 계정·업체의 시연 중 작업만 초기화하고 과거 시드 이력을 유지합니다.
일반 사용자나 다른 업체 데이터는 초기화 대상에 포함하지 않습니다.

## 구조화 정책과 판정

`requestedPolicy.conditions`에 아래 형태를 추가합니다. 기존 세 규칙 목록도 유지합니다.

```json
{
  "petsAllowed": true,
  "indoor": {"allowed": true, "maxWeightKg": 10, "equipmentCodes": ["STROLLER"]},
  "outdoor": {"allowed": true, "maxWeightKg": null, "equipmentCodes": ["LEASH"]},
  "effectiveFrom": "2026-11-18",
  "notice": "실내 10kg 이하, 초과 시 야외 테라스 이용",
  "changeReason": "매장 운영 기준 변경",
  "vaccinationConfirmed": false
}
```

조건 변경 요청은 승인 전 지도에 반영되지 않습니다. 승인된 `store_policy`를 업체 웹과
사용자 지도가 함께 읽습니다. 적용 시작일은 한국 날짜로 판단하며 미래 버전은 적용일까지
기존 정책을 유지합니다. 기존 `place_entry_policy`는 공공데이터·레거시 정책의 대체값입니다.
구역이 지정되면 그 구역의 장비 조건을 사용하고, 견종/접종 조건은 공통으로 적용합니다.
'함께 가요'는 두 반려견이 같은 구역을 이용할 수 있는지 판정합니다.

| API | 추가 동작 |
| --- | --- |
| `POST /business/stores/{id}/policy-preview` | 요청한 정책과 현재 정책으로 8kg 유모차 반려견 예시를 같은 판정기로 비교 |
| `GET /places/map`, `/places` | 카드에 category 추가 |
| `GET /places/{id}` | category, phoneNumber, imageUrls, businessHours, conditions, maxWeightKg, requiredEquipmentCodes, entryArea |
| `GET /business/stores/{id}/visit-stats` | guardianCount, dailyAverage, byHour, byWeekday; 한국 시간 기준 |
| `GET /business/stores/{id}/dashboard` | 이번 달 인증/스탬프·진행 중 요청·조건 확인일 |
| `GET /business/events?placeId={id}` | 업체가 볼 수 있는 모집·참여 이벤트 |
| `GET /business/events/{id}?placeId={id}` | 실제 기간·모집 기간·참여 조건·경품·내 신청 |
| `GET /business/faqs` | 운영 FAQ |
| `POST /files` | multipart `file`, 인증 사용자 소유 파일 업로드 |
| `POST /business/stores/{id}/images` | 관리 업체의 대표 이미지 업로드 |
| `GET /files/{assetId}/url` | 소유자/운영자에게 새 signed URL 반환 |
| `POST /inquiries` | `relatedRequest: {type: AD/EVENT/CONDITION, id}` 및 업로드 파일 참조 |
| `POST /chat/sessions/{id}/messages` | 명시적인 반려견 준비물 추가 명령에 profileAction 제안 |
| `POST /chat/sessions/{id}/profile-actions/{actionId}/confirm` | 소유권·10분 유효기간을 확인하고 한 번만 반영 |

QR은 서버가 원본 토큰으로 실제 PNG를 만들고 Storage에 업로드합니다.
DB에는 만료되는 URL 대신 `storage://` 참조를 저장하고 조회 시 signed URL을 생성합니다.
일반 첨부도 `/api/v1/files/{uuid}` 참조를 저장하고 소유권을 검사합니다.
AI 응답 내용은 반려견 수정 명령으로 실행하지 않습니다.

## 검증 범위

백엔드 회귀/통합 테스트, PNG 디코딩으로 QR 토큰 확인, 파일·반려견 소유권 테스트,
임시 PostgreSQL의 전체 Flyway 적용과 API 시드/재실행/초기화를 검증합니다.
업체 웹은 TypeScript·Vite 빌드와 lint를 실행합니다.
iOS는 Xcode가 있는 환경에서 추가로 시뮬레이터 빌드·실기기 QR 촬영을 확인해야 합니다.
