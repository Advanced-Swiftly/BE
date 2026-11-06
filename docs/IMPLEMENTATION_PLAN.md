# 구현 순서

## 0. 공통 기반

- [x] Spring Boot / Gradle / Java 프로젝트
- [x] PostgreSQL, Flyway, local Docker Compose
- [x] Security 기본 정책, CORS, 오류 응답, request id
- [x] Swagger UI, Actuator
- [x] GitHub Actions CI/CD, Render Blueprint
- [ ] ERD를 PostgreSQL 타입으로 검토하고 Flyway migration 작성
- [ ] 프론트엔드와 공통 응답·페이지네이션·파일 업로드 규격 확정

## 1. 인증과 기준 정보

- 공통·인증 10개
- 회원 5개
- 약관, 지역, 견종 seed data
- Access/Refresh token rotation과 로그아웃

완료 조건: 회원가입부터 로그인, 토큰 재발급, 탈퇴까지 통합 테스트가 통과합니다.

## 2. 반려견과 예방접종

- 반려견 6개
- 예방접종 5개
- Supabase Storage private bucket
- OCR adapter와 운영자 검토 상태 모델

완료 조건: 문서 업로드 URL이 외부에 공개되지 않고, OCR 실패·재제출 흐름을 재현할 수 있습니다.

## 3. 장소와 공공데이터

- 공공데이터 5개
- 장소·판정 4개
- 공공시설 수집·정규화·중복 제거
- 업체 정책과 선택견의 최악 우선 입장 판정

완료 조건: 같은 입력에 판정 결과와 근거가 결정적으로 반환되고, 동기화 재실행이 중복 데이터를 만들지 않습니다.

## 4. 챗봇, 문의, 방문·이벤트

- 챗봇 5개
- 문의 3개
- 방문·이벤트 8개
- QR 재사용 방지와 스탬프 중복 적립 방지

완료 조건: 동시 요청에서도 방문·응모·스탬프가 한 번만 처리되고, 챗봇 장애가 핵심 API를 막지 않습니다.

## 협업 규칙

- DB migration 번호는 PR 전에 백엔드 담당자끼리 예약합니다.
- 공통 계약 변경은 프론트엔드 두 명에게 먼저 공유합니다.
- 외부 API는 interface 뒤에 두고 테스트에서는 fake adapter를 사용합니다.
- 각 PR은 한 도메인 또는 한 인프라 변경을 원칙으로 합니다.
- [담당 API 목록](API_SCOPE.md)의 Notion 원문을 acceptance criteria로 사용합니다.
