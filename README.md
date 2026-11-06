# PawFit Backend

반려견과 함께 갈 수 있는 장소를 찾고, 업체 정책과 반려견 정보를 비교해 입장 가능 여부를 판정하는 PawFit의 Spring Boot API입니다.

## 기술 스택

- Java 21 / Spring Boot 4.1.2 / Gradle 9.8.2
- Spring MVC, Security, Validation, JPA, Flyway
- PostgreSQL (local Docker, production Supabase)
- OpenAPI / Swagger UI
- Actuator
- Docker / Render
- GitHub Actions

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
| `JWT_SECRET` | JWT 서명 키(인증 구현 시 사용) | 배포 |
| `SUPABASE_URL` | Supabase 프로젝트 URL | 파일 기능 |
| `SUPABASE_SERVICE_ROLE_KEY` | 서버 전용 Storage 키 | 파일 기능 |
| `SUPABASE_STORAGE_BUCKET` | Storage bucket 이름 | 파일 기능 |
| `GEMINI_API_KEY` | OCR·챗봇 API 키 | AI 기능 |

실제 비밀값은 Git이나 설정 파일에 커밋하지 않습니다.

## 문서

- [담당 API 9개 도메인·51개 목록](docs/API_SCOPE.md)
- [아키텍처와 패키지 규칙](docs/ARCHITECTURE.md)
- [권장 구현 순서](docs/IMPLEMENTATION_PLAN.md)
- [Supabase·Render·GitHub Actions 배포](docs/DEPLOYMENT.md)
- [Notion API 명세서](https://app.notion.com/p/40e938594612806594b7cf3ffb1f1cc8)

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
