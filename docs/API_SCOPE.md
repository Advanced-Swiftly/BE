# API 범위

원본: [Notion API 명세서](https://app.notion.com/p/40e938594612806594b7cf3ffb1f1cc8)

2026-11-08 기준 Notion의 13개 카테고리, 총 **115개 엔드포인트**가 구현 기준입니다.
이 문서의 상세 표는 사용자 앱·공통 영역 9개 도메인, 51개 엔드포인트를 정리합니다.
업체 인증·업체 운영·업체 신청·운영자 심사의 64개 엔드포인트는 Notion 원문을
단일 기준으로 사용하며, 구현 PR에서 필요한 항목을 이 문서에 추가합니다.

| 도메인 | API 수 |
| --- | ---: |
| 공통·인증 | 10 |
| 회원 | 5 |
| 반려견 | 6 |
| 예방접종 | 5 |
| 장소·판정 | 4 |
| 챗봇 | 5 |
| 문의 | 3 |
| 방문·이벤트 | 8 |
| 공공데이터 | 5 |
| 합계 | 51 |

## 공통·인증 (10)

| Method | Path | 기능 | Notion |
| --- | --- | --- | --- |
| POST | `/api/v1/auth/login` | 로그인 | [명세](https://app.notion.com/40e9385946128122bf7ffac0f6216426) |
| POST | `/api/v1/auth/logout` | 로그아웃 | [명세](https://app.notion.com/40e938594612814bb82df45420ac5e5b) |
| POST | `/api/v1/auth/phone-verifications` | 휴대전화 인증번호 발송 | [명세](https://app.notion.com/40e9385946128179aa8dda92aeb088b9) |
| POST | `/api/v1/auth/phone-verifications/confirm` | 휴대전화 인증번호 확인 | [명세](https://app.notion.com/40e93859461281e9baebdf05c814d806) |
| POST | `/api/v1/auth/signup/business` | 업체관리자 회원가입 | [명세](https://app.notion.com/40e938594612817d9b66e0caec49f7c2) |
| POST | `/api/v1/auth/signup/guardian` | 보호자 회원가입 | [명세](https://app.notion.com/40e9385946128168857cd0c6fa403053) |
| POST | `/api/v1/auth/token/refresh` | 액세스 토큰 재발급 | [명세](https://app.notion.com/40e93859461281edb72ff12ff04b2491) |
| GET | `/api/v1/breeds` | 견종 목록 조회 | [명세](https://app.notion.com/40e93859461281759472f453562b8d1b) |
| GET | `/api/v1/regions` | 지역 목록 조회 | [명세](https://app.notion.com/40e9385946128170bae0c499f1a7703d) |
| GET | `/api/v1/terms` | 이용약관 목록 조회 | [명세](https://app.notion.com/40e938594612818dbd43e97d3290cf78) |

## 회원 (5)

| Method | Path | 기능 | Notion |
| --- | --- | --- | --- |
| DELETE | `/api/v1/users/me` | 회원 탈퇴 | [명세](https://app.notion.com/40e93859461281bbb0f7ca87df8d836b) |
| GET | `/api/v1/users/me` | 내 프로필 조회 | [명세](https://app.notion.com/40e93859461281afa761e1638fd29770) |
| PATCH | `/api/v1/users/me` | 내 프로필 수정 | [명세](https://app.notion.com/40e9385946128196982cd15311061d5f) |
| GET | `/api/v1/users/me/notification-settings` | 알림 설정 조회 | [명세](https://app.notion.com/40e938594612815784c7da8080918506) |
| PATCH | `/api/v1/users/me/notification-settings` | 알림 설정 변경 | [명세](https://app.notion.com/40e93859461281bb887de326bf55a770) |

## 반려견 (6)

| Method | Path | 기능 | Notion |
| --- | --- | --- | --- |
| GET | `/api/v1/dogs` | 내 반려견 목록 조회 | [명세](https://app.notion.com/40e93859461281838a44e5b92c0df861) |
| POST | `/api/v1/dogs` | 반려견 등록 | [명세](https://app.notion.com/40e938594612812ab86addc44ced761b) |
| DELETE | `/api/v1/dogs/{dogId}` | 반려견 삭제 | [명세](https://app.notion.com/40e9385946128168a80ececb2b00edc0) |
| GET | `/api/v1/dogs/{dogId}` | 반려견 상세 조회 | [명세](https://app.notion.com/40e93859461281b3945dea929502d218) |
| PATCH | `/api/v1/dogs/{dogId}` | 반려견 정보 수정 | [명세](https://app.notion.com/40e9385946128120b3abdbb9a2e2e1a6) |
| PATCH | `/api/v1/dogs/{dogId}/default` | 대표 반려견 설정 | [명세](https://app.notion.com/40e938594612813f95b4fa1ca3275845) |

## 예방접종 (5)

| Method | Path | 기능 | Notion |
| --- | --- | --- | --- |
| POST | `/api/v1/dogs/{dogId}/vaccination-documents` | 예방접종 증명서 업로드 | [명세](https://app.notion.com/40e93859461281c693a3c8d9b585b815) |
| GET | `/api/v1/dogs/{dogId}/vaccination-record` | 예방접종 인증 현황 조회 | [명세](https://app.notion.com/40e93859461281bbaf99d6c89d0c2135) |
| GET | `/api/v1/vaccination-records/{recordId}` | 예방접종 분석 결과 조회 | [명세](https://app.notion.com/40e93859461281a7bd23f6538dd7ec23) |
| PATCH | `/api/v1/vaccination-records/{recordId}` | 예방접종 분석값 수정 | [명세](https://app.notion.com/40e938594612818d9460f35d9ea61164) |
| POST | `/api/v1/vaccination-records/{recordId}/resubmit` | 예방접종 증명서 재제출 | [명세](https://app.notion.com/40e93859461281e8ac66f3097b2724f9) |

## 장소·판정 (4)

| Method | Path | 기능 | Notion |
| --- | --- | --- | --- |
| GET | `/api/v1/places` | 장소 검색 | [명세](https://app.notion.com/40e93859461281f5a064e7c2e56469a7) |
| GET | `/api/v1/places/map` | 지도 장소 조회 | [명세](https://app.notion.com/40e9385946128160b657df4a5f083e09) |
| GET | `/api/v1/places/{placeId}` | 장소 상세 조회 | [명세](https://app.notion.com/40e93859461281859d0ad16f5a7b0cea) |
| POST | `/api/v1/places/{placeId}/entry-judgments` | 선택견 입장 가능 여부 판정 | [명세](https://app.notion.com/40e9385946128169b283c6c4809dfd8f) |

## 챗봇 (5)

| Method | Path | 기능 | Notion |
| --- | --- | --- | --- |
| GET | `/api/v1/chat/sessions` | 챗봇 세션 목록 조회 | [명세](https://app.notion.com/40e938594612815f9f88e1b9fa5a1ac8) |
| POST | `/api/v1/chat/sessions` | 챗봇 세션 생성 | [명세](https://app.notion.com/40e93859461281278f81d4b838de08d1) |
| DELETE | `/api/v1/chat/sessions/{sessionId}` | 챗봇 세션 삭제 | [명세](https://app.notion.com/40e93859461281ceb238e5b14d8aab36) |
| GET | `/api/v1/chat/sessions/{sessionId}/messages` | 챗봇 메시지 내역 조회 | [명세](https://app.notion.com/40e938594612814fb83fcfc9ec698146) |
| POST | `/api/v1/chat/sessions/{sessionId}/messages` | 챗봇 메시지 전송 | [명세](https://app.notion.com/40e9385946128165884ecb4cf1bcb8f6) |

## 문의 (3)

| Method | Path | 기능 | Notion |
| --- | --- | --- | --- |
| GET | `/api/v1/inquiries` | 내 문의 목록 조회 | [명세](https://app.notion.com/40e9385946128131b004d0935b31bb3b) |
| POST | `/api/v1/inquiries` | 1:1 문의 등록 | [명세](https://app.notion.com/40e93859461281f3acc1fbffa051538a) |
| GET | `/api/v1/inquiries/{inquiryId}` | 문의 상세 조회 | [명세](https://app.notion.com/40e93859461281b6ad4bf6f5a0444b9e) |

## 방문·이벤트 (8)

| Method | Path | 기능 | Notion |
| --- | --- | --- | --- |
| GET | `/api/v1/events/current` | 현재 이벤트 조회 | [명세](https://app.notion.com/40e93859461281a881abc7df4313de5a) |
| GET | `/api/v1/events/{eventId}` | 이벤트 상세 조회 | [명세](https://app.notion.com/40e93859461281ae88e2d6706b196379) |
| POST | `/api/v1/events/{eventId}/draw-entries` | 이벤트 추첨 응모 | [명세](https://app.notion.com/40e938594612813285bfcc52793410ff) |
| GET | `/api/v1/events/{eventId}/draw-entry` | 내 추첨 응모·결과 조회 | [명세](https://app.notion.com/40e938594612817d8b51fe2b8471322f) |
| GET | `/api/v1/events/{eventId}/stamps` | 이벤트 스탬프 현황 조회 | [명세](https://app.notion.com/40e93859461281c09b85d71b86d67d85) |
| GET | `/api/v1/events/{eventId}/stores` | 이벤트 참여 업체 목록 조회 | [명세](https://app.notion.com/4399385946128246a29f81eab5c25364) |
| GET | `/api/v1/visits` | 내 방문 기록 조회 | [명세](https://app.notion.com/40e9385946128106b4dbe64a5bbd32ee) |
| POST | `/api/v1/visits/qr` | 업체 QR 방문 인증 | [명세](https://app.notion.com/40e93859461281ef910cf873f87e1ca3) |

## 공공데이터 (5)

| Method | Path | 기능 | Notion |
| --- | --- | --- | --- |
| GET | `/api/v1/admin/public-data-sources` | 공공데이터 원천 목록 조회 | [명세](https://app.notion.com/40e93859461281ad9252c7e42ebfe629) |
| POST | `/api/v1/admin/public-data-sources/{sourceId}/sync` | 공공데이터 수동 동기화 | [명세](https://app.notion.com/40e93859461281b593bedfd96f1bf165) |
| GET | `/api/v1/admin/public-data-sources/{sourceId}/sync-runs` | 공공데이터 동기화 이력 조회 | [명세](https://app.notion.com/f1793859461283f6b12e01f6bd9bbe35) |
| GET | `/api/v1/admin/public-facilities` | 공공시설 목록 조회 | [명세](https://app.notion.com/40e93859461281fbb71bc1d7d19beaf6) |
| PATCH | `/api/v1/admin/public-facilities/{facilityId}` | 공공시설 정보 보정 | [명세](https://app.notion.com/40e9385946128118a11edcfd0c86e33a) |


