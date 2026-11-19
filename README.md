# PawFit Backend

반려견과 함께 갈 수 있는 장소를 찾고, 업체 정책과 반려견 정보를 비교해 입장 가능 여부를 판정하는 PawFit의 Spring Boot API입니다.

## 기술 스택

- Java 21 / Spring Boot 4.1.2 / Gradle 9.8.2
- Spring MVC, Security, Validation, JPA, Flyway
- PostgreSQL (local Docker, production Supabase)
- OpenAPI / Swagger UI
- Actuator
- Docker / Render
- GitHub Actions / CodeRabbit

## 빠른 시작

요구 사항: JDK 21+, Docker

```bash
cp .env.example .env
docker compose up -d postgres
./gradlew bootRun
```

확인:

- Health: http://localhost:8080/actuator/health
- Swagger UI: http://localhost:8080/swagger-ui.html
- OpenAPI JSON: http://localhost:8080/v3/api-docs

테스트와 빌드:

```bash
./gradlew clean test bootJar
docker build -t pawfit-api .
```

`.env`는 Docker Compose가 읽습니다. 애플리케이션 환경 변수를 현재 shell에도 적용하려면 별도로 export하거나 IDE Run Configuration에 등록합니다. 기본 local 설정은 별도 입력 없이 `localhost:5432/pawfit`에 연결됩니다.

## 설정

| 환경 변수 | 용도 | 필수 환경 |
| --- | --- | --- |
| `SPRING_PROFILES_ACTIVE` | `local`, `test`, `prod` | 배포 |
| `DB_URL` | JDBC PostgreSQL URL | 배포 |
| `DB_USERNAME` | DB 사용자 | 배포 |
| `DB_PASSWORD` | DB 비밀번호 | 배포 |
| `CORS_ALLOWED_ORIGINS` | 쉼표 구분 프론트엔드 origin | 배포 |
| `JWT_SECRET` | JWT 서명 키(32 byte 이상) | 배포 |
| `PHONE_VERIFICATION_DEMO_ENABLED` | 시연용 고정 인증번호 사용 여부 | 시연 배포 |
| `PHONE_VERIFICATION_DEMO_CODE` | 시연용 6자리 인증번호 | 시연 배포 |
| `SUPABASE_URL` | Supabase 프로젝트 URL | 파일 기능 |
| `SUPABASE_SERVICE_ROLE_KEY` | 서버 전용 Storage 키 | 파일 기능 |
| `SUPABASE_STORAGE_BUCKET` | Storage bucket 이름 | 파일 기능 |
| `AI_PROVIDER` | 기본 `GEMINI`, 선택 `WORKERS_AI` | AI 기능 |
| `AI_TIMEOUT`, `AI_MAX_RETRIES` | AI timeout·재시도 | AI 기능 |
| `GEMINI_API_KEY` | Gemini OCR·챗봇 API 키 | Gemini 선택 시 |
| `GEMINI_CHAT_FALLBACK_MODEL` | 채팅 일시 오류 시 대체 모델(기본 `gemini-3.5-flash-lite`, 빈 값으로 끔) | Gemini 선택 시 |
| `NTS_BUSINESS_STATUS_SERVICE_KEY` | 국세청 사업자 상태조회 Decoding 인증키 | 사업자 상태조회 |
| `NTS_BUSINESS_STATUS_TIMEOUT` | 국세청 API timeout(기본 5초) | 사업자 상태조회 |
| `CLOUDFLARE_ACCOUNT_ID`, `CLOUDFLARE_API_TOKEN` | Workers AI 인증 | Workers AI 선택 시 |

운영 배포는 이미지·PDF 이해와 구조화 출력 품질을 위해 `GEMINI`와
`gemini-3.8-flash`를 사용합니다. Workers AI 구현은 대체 provider로 유지합니다.
일반 채팅은 기본 모델의 과부하·요청 제한·일시 통신 오류가 발생하면 Flash-Lite로
한 번 전환합니다. 두 모델이 모두 실패하면 기존 제한 횟수만큼 재시도합니다.
인증·권한·잘못된 요청 오류는 전환하지 않으며, 예방접종 분석은 기존 모델을 유지합니다.

실제 비밀값은 Git이나 설정 파일에 커밋하지 않습니다.
현재 시연 배포는 휴대전화 인증번호를 `123456`으로 고정합니다. 실제 운영으로 전환할
때는 `PHONE_VERIFICATION_DEMO_ENABLED=false`로 변경하고 SMS provider를 연결해야 합니다.

## 문서

- [Notion 기준 115개 API 범위](docs/API_SCOPE.md)
- [아키텍처와 패키지 규칙](docs/ARCHITECTURE.md)
- [권장 구현 순서](docs/IMPLEMENTATION_PLAN.md)
- [Supabase·Render·GitHub Actions 배포](docs/DEPLOYMENT.md)
- [AI·Storage 정책과 실패 처리](docs/AI_AND_STORAGE.md)
- [업체 프론트 누락 API 요청·응답과 재연결 가이드](docs/PARTNER_API_INTEGRATION.md)
- [Render·Supabase 배포 준비 단계](배포준비단계.md)
- [Notion API 명세서](https://app.notion.com/p/40e938594612806594b7cf3ffb1f1cc8)

## Code Review

CodeRabbit Open Source 무료 Review를 사용합니다. GitHub App 권한은 `Advanced-Swiftly/BE` Repository에만 부여합니다. `develop`과 `main` 대상 PR은 자동 Review하며, 세부 기준은 [CodeRabbit 설정](.coderabbit.yaml)에 정의합니다. 자동 Approval은 사용하지 않으므로 최종 Merge 전에는 백엔드 팀원의 Review가 필요합니다.

## 브랜치와 협업

- `main`: 최종 배포 가능 버전
- `develop`: 기능 통합 및 Render 배포 브랜치
- `{type}/#{issue-number}`: 이슈 단위 작업 브랜치

작업 흐름:

1. GitHub Issue 생성
2. 최신 `develop`에서 작업 브랜치 생성
3. 테스트 통과 후 `develop` 대상 PR
4. 다른 백엔드 팀원의 승인 후 merge

PR에서는 CI의 `Test and build` 검사를 반드시 통과해야 합니다.

## 시나리오 시연

[시연 초기 상태·구조화 정책·명시적 시드 및 초기화](docs/DEMO_SCENARIO.md)를 참고하세요.
[배포 장소 결과·실제 조사 41곳·재입력 검증](docs/PLACE_DATA_RELEASE.md)과
[동반 확인 근거 입력](docs/confirmed-admission-import.md)은 현재 지도 노출 기준을 설명합니다.
[제한 없는 초록색 시연 업체 9곳](docs/UNRESTRICTED_DEMO_PLACES.md)은 별도 시드로 추가합니다.
`python3 scripts/demo_seed.py plan`은 DB에 연결하지 않고 입력 계획을 보여 줍니다.
