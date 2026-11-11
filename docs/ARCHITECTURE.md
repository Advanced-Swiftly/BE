# PawFit 백엔드 아키텍처

## 기준

- Java 21, Spring Boot 4.1
- 도메인 중심 패키지 구조
- PostgreSQL + Flyway
- Stateless Spring Security
- REST API prefix: `/api/v1`
- 시간 저장: UTC `Instant`
- Render Spring Boot 서비스 1개에서 보호자·업체·운영자를 모듈과 권한으로 분리

## 패키지 구조

```text
com.advancedswiftly.pawfit
├── global
│   ├── common       # 공통 엔티티와 응답
│   ├── config       # Security, JPA, OpenAPI
│   └── error        # 예외, 오류 응답, request id
├── auth             # 공통·인증
├── user             # 회원
├── dog              # 반려견
├── vaccination      # 예방접종
├── place            # 장소·판정
├── chat             # 챗봇
├── inquiry          # 문의
├── event            # 방문·이벤트
├── publicdata       # 공공데이터
├── business         # 업체 인증·운영·신청
└── admin            # 운영자 심사
```

각 도메인은 필요한 계층만 만듭니다.

```text
domain
├── api              # Controller, request/response DTO
├── application      # Use case, transaction boundary
├── domain           # Entity, value object, domain policy
└── infrastructure   # JPA repository, external API adapter
```

## 의존성 규칙

1. Controller는 application 계층만 호출합니다.
2. JPA Entity를 API 응답으로 직접 반환하지 않습니다.
3. 다른 도메인의 Repository를 직접 호출하지 않고 application service를 경유합니다.
4. 외부 서비스(Supabase Storage, Gemini, 공공데이터)는 interface와 adapter로 분리합니다.
5. ERD 확정 전 초기 개발은 `ddl-auto=update`를 사용합니다. 전체 migration이 준비되면
   운영 환경을 `validate`로 전환하고 이후 변경은 Flyway로만 반영합니다.
6. 새 API는 정상·권한·검증 실패 테스트와 OpenAPI 설명을 함께 추가합니다.

## 데이터베이스 변경

`src/main/resources/db/migration`에 다음 규칙으로 추가합니다.

```text
V1__create_users.sql
V2__create_dogs.sql
V3__add_user_notification_settings.sql
```

이미 적용된 migration 파일은 수정하지 않습니다. 수정이 필요하면 다음 버전 파일을 추가합니다.
