# 업체 프론트 누락 API 연동 가이드 (#55)

FE `develop` 커밋 `96caf409ae97fe65c9249c32ef4c3557f43607f2`의
`web/partner/src/api/account.ts`, `promo.ts` 및 업체 화면과 대조했습니다.
모든 경로 앞에는 `/api/v1`이 붙습니다. PR의 develop 병합과 Render 배포 성공 후 사용할 수 있습니다.

## 요청·응답

| Method | Path | 인증 | 요청 → 응답 |
| --- | --- | --- | --- |
| POST | `/business/auth/login-id/check` | 공개 | `{ "loginId": "owner@example.com" }` → `{ "available": true }` |
| POST | `/business/verifications/business-status` | BUSINESS | `{ "businessNumber": "1234567890" }` → `{ "isActive": true }` |
| GET | `/business/events/{eventId}/eligibility?placeId=11` | BUSINESS·관리 업체 | 참여조건·기존 신청·canApply |
| PATCH | `/business/applications/{applicationId}` | BUSINESS·본인 신청·관리 업체 | `{ "startDate": "2026-12-07", "weeks": 2 }` → 광고 신청 모델 |
| PATCH | `/users/me/password` | 로그인 | `{ "currentPassword": "...", "newPassword": "..." }` → 204 |

### 이메일 아이디

- 이메일을 소문자로 정규화합니다. 탈퇴한 계정의 아이디도 재사용하지 않습니다.
  중복 확인은 예약이 아니며 가입 시 재검사하고 DB 고유 제약으로 동시 중복을 차단합니다.
- 기존 전화번호 가입·로그인은 그대로 지원합니다. `V21`은 기존 계정에 임의 이메일을 부여하지 않습니다.
- `POST /auth/signup/business`에 선택 필드 `loginId`를 추가했습니다.
  `POST /auth/login`은 `phoneNumber` 또는 `loginId` 중 하나만 받습니다.
  예: `{ "loginId": "owner@example.com", "password": "..." }`.
- `GET /users/me`에도 저장된 `loginId`를 제공합니다. 이메일 없이 가입한 계정에는 이 필드가 없습니다.
- 잘못된 입력 400, 기존 이메일 `409 DUPLICATE_LOGIN_ID`, 동시 가입 고유 제약 위반 409,
  로그인 실패 `401 INVALID_CREDENTIALS`입니다.
- 프론트 변경: `SignupAccountPage.tsx`의 서버 모드 이메일 입력·중복 확인을 활성화하고,
  `createAccount()`에 `loginId: form.loginId.trim()`을 포함합니다.
  이메일 로그인은 `digitsOnly()` 변환 없이 loginId로 보내고, 계정 정보는 프로필의 loginId를 사용합니다.

### 사업자등록 상태조회

- 숫자 10자리와 `123-45-67890` 형식을 받습니다. 국세청 계속사업자(`01`)만 true이며 휴업·폐업·미등록은 false입니다.
  이 결과는 PawFit 업체 심사 승인을 의미하지 않습니다.
- 국세청 서비스 활용 승인을 받은 **Decoding 인증키**를 Render의
  `NTS_BUSINESS_STATUS_SERVICE_KEY`에 등록해야 합니다. 관광공사 인증키의 국세청 권한은 별도입니다.
  `NTS_BUSINESS_STATUS_TIMEOUT` 기본값은 5초입니다.
- 키 미설정·HTTP 오류·timeout·잘못된 응답은 `503 BUSINESS_STATUS_UNAVAILABLE`입니다.
  장애를 영업 상태 true/false로 대체하지 않습니다. 입력 오류 400, 미인증 401, 다른 역할 403입니다.
- 프론트 변경: 현재 `ServerBusinessForm.tsx`에 없는 상태조회 UI를 `lookupBusiness()`에 연결합니다.
- 공식 명세: [공공데이터포털](https://www.data.go.kr/data/15081808/openapi.do),
  [OpenAPI JSON](https://infuser.odcloud.kr/api/stages/28493/api-docs).

### 이벤트 참여조건

응답 필드는 `eventId`, `placeId`, `baseDate`, `recruitmentOpen`, `existingApplication`,
`checks`, `allPassed`, `canApply`입니다. ID는 프론트 모델에 맞춘 문자열입니다.
existingApplication은 null 또는 `{ "applicationId": "123", "status": "RECEIVED" }`입니다.
checks는 다음 네 항목의 `{ code, passed, detail? }` 목록입니다.

- REGION: 업체의 region_id가 이벤트 지역에 포함됩니다.
- BUSINESS_VERIFIED: 업체 인증 상태가 VERIFIED입니다.
- QR_ACTIVE: 가장 최근 QR이 ACTIVE이고 방문 인증용 토큰 해시가 있습니다.
- CONDITION_CHECKED_RECENT: 최신 적용 정책의 생성일(정책이 승인되어 적용된 시점)이 한국 날짜로 0~30일 전입니다.
  별도 임의 확인일을 만들지 않습니다. detail은 `{ checkedDate, daysSince, reason? }`이며
  미확인·미래·31일 이상은 각각 NO_DATE, FUTURE_DATE, OVER_30_DAYS입니다.

allPassed는 네 조건의 결과입니다. canApply는 조건 충족·모집 중·활성 신청 없음까지 확인합니다.
반려·취소된 신청만 재신청할 수 있습니다. 별도 모집 기간 필드가 없어 기존 OPEN 상태와
이벤트 시작일~종료일(한국 날짜, 양 끝 포함)을 모집 기간으로 사용합니다.
관리 업체 이외 접근 403, 없는 이벤트 404, 누락·잘못된 placeId는 400입니다.

프론트 변경: `ApplyEventModal.tsx`에서 서버 모드의 getEligibility() 호출 차단을 해제하고
canApply로 버튼을 제어합니다. 실제 신청에서도 같은 조건을 다시 검사합니다.
`400 EVENT_NOT_ELIGIBLE`이면 조건을 새로 조회합니다.

### 광고 기간 수정

- applicationId는 **광고 신청 ID**입니다. 이벤트 신청 ID와 공용 식별자가 아닙니다.
- RECEIVED 상태만 수정할 수 있습니다. 심사 시작 이후 `400 REQUEST_NOT_EDITABLE`,
  타인의 신청 404, 관리 권한을 잃은 업체의 기간 수정은 403입니다.
- startDate·weeks 중 하나 이상을 전달합니다. weeks는 프론트 선택지와 같은 정수 1~4입니다.
  종료일은 `시작일 + 주 수 × 7 - 1일`입니다. 예시 요청은 2026-12-20에 종료됩니다.
  시작일만 바꾸면 기존 일수를, 주 수만 바꾸면 기존 시작일을 유지합니다.
- `/business/applications/{id}` 응답은 프론트 ApplicationDto의 applicationId, type=AD,
  placeId, placeName, status, displayStatus, submittedAt, creativeUrl, ad, history,
  rejectReason, canCancel, canEditPeriod, canReapply입니다. ID는 문자열입니다.
  ad는 `{ region, startDate, weeks, endDate }`입니다. 저장된 지역 문자열이 없어 region은 null입니다.
  기존 기간이 정확한 주 단위가 아니면 시작일만 바꾼 결과의 weeks도 null입니다.
  날짜 범위를 우선 표시하고 null을 임의 값으로 대체하지 마세요.
  history는 확인 가능한 접수·취소 기록이며 전체 심사 이력은 아닙니다.
- 기존 `PATCH /business/ad-applications/{id}`에서도 기간 요청을 받습니다.
  기존 `{ "action": "CANCEL" }` 취소 및 숫자 applicationId·status 응답은 유지합니다.
  이 경로의 기간 수정 응답도 ID·status 형식이며 날짜는 상세 조회로 확인합니다.
  취소와 기간 변경을 섞거나 빈 객체를 보내면 400입니다.
- 광고 상세 조회에 storeId, adType, startDate, endDate, message, canEditPeriod를 추가했습니다.
  프론트 `fromMyApplication()`의 canEditPeriod=false 고정값을 응답값으로 바꿔 버튼을 활성화합니다.
- 기간 변경·취소와 운영자 심사는 같은 DB 행 잠금을 사용합니다.

### 비밀번호 변경

- 새 비밀번호는 8~72자, 영문·특수문자 포함, UTF-8 기준 72 byte 이하입니다.
  현재와 같은 비밀번호는 400, 잘못된 현재 비밀번호는 `401 INVALID_PASSWORD`입니다.
- 성공 시 계정의 모든 refresh token을 폐기합니다. 기존 access token은 원래 만료 시점까지
  유효합니다(기본 30분). 모든 access token의 즉시 폐기는 포함하지 않습니다.
- 프론트 변경: `changePassword()`의 FIELD_NOT_SUPPORTED 차단을 PATCH 요청으로 바꾸고,
  성공 후 브라우저 세션을 지워 로그인 화면으로 이동합니다. INVALID_PASSWORD로 token 갱신을 시도하지 않습니다.

## 배포와 확인

1. 새 Flyway V21을 적용하고 Render에 국세청 인증키를 등록합니다.
2. develop 병합 후 CI 및 Render 배포 성공을 확인합니다.
3. 프론트의 비활성화된 요청과 이메일·광고 응답 매핑을 위 설명대로 연결합니다.
4. 테스트 계정으로 가입→로그인→사업자조회→업체승인→정책·QR 준비→이벤트신청→광고수정→비밀번호변경을 확인합니다.

국세청 테스트는 외부 응답을 통제하는 테스트이며 실제 발급 키로 실호출하는 검증은 별도입니다.
