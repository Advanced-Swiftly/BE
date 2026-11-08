# 배포 설정

## 1. Supabase

1. Free 프로젝트를 생성합니다.
2. Dashboard의 **Connect**에서 **Session pooler**와 **JDBC**를 선택합니다.
3. 포트 5432의 값을 아래 Render 환경 변수에 분리해 저장합니다.
   - `DB_URL`: `jdbc:postgresql://<pooler-host>:5432/postgres?sslmode=require`
   - `DB_USERNAME`: `postgres.<project-ref>`
   - `DB_PASSWORD`: 데이터베이스 비밀번호
4. Storage에 private bucket `pawfit`을 생성합니다.
   또는 SQL Editor에서 `supabase/setup-storage.sql`을 실행합니다.
5. 서비스 역할 키는 백엔드의 `SUPABASE_SERVICE_ROLE_KEY`에만 저장하고 앱이나 Git에는 넣지 않습니다.

Render는 장기 실행 컨테이너이며 Free 환경에서 IPv4 연결이 필요하므로 Supabase Session pooler를 사용합니다. Hikari 최대 연결 수는 기본 5로 제한했습니다.

Supabase의 `public` schema에는 새 프로젝트에서도 system object가 존재할 수 있습니다.
운영 Flyway는 최초 실행 시 version 0 baseline을 만든 뒤 V1부터 migration을 적용하며,
이후 배포에서는 `flyway_schema_history`를 기준으로 일반 migration만 실행합니다.

## 2. Render

1. Render Dashboard에서 **New > Blueprint**를 선택합니다.
2. `develop` 브랜치가 원격에 생성되어 있고 초기 세팅이 포함됐는지 확인합니다.
3. GitHub의 `Advanced-Swiftly/BE` 저장소를 연결합니다.
4. 루트의 `render.yaml`을 적용합니다.
5. `sync: false` 환경 변수를 입력합니다.
6. 첫 Blueprint 생성 후 `develop`의 GitHub Check가 Render에 표시되는지 확인합니다.

`render.yaml`은 Singapore, Free, Docker, `develop` 브랜치를 사용합니다.
`autoDeployTrigger: checksPass`이므로 GitHub Actions 검사가 성공한 커밋만 Render가 배포합니다.

## 3. GitHub Actions

흐름:

1. PR 및 `main`/`develop` push에서 테스트, JAR 빌드, Docker 이미지 빌드
2. `develop` CI 성공 상태를 Render가 확인
3. Render가 새 커밋을 빌드하고 `/actuator/health`로 상태 확인

브랜치 보호 규칙에서 `Backend CI / Test and build`를 필수 검사로 설정하는 것을 권장합니다.

## 4. UptimeRobot

시연 기간에만 배포 URL의 `https://<service>.onrender.com/actuator/health`를 HTTP(s)
monitor로 등록합니다. 상시 모니터링이 필요하면 Render 유료 인스턴스를 사용합니다.

## 5. 운영 점검

- `/actuator/health`: Render/UptimeRobot 상태 확인
- `/actuator/info`: 빌드 버전 확인
- Swagger/OpenAPI: local에서만 활성화하며 prod에서는 비활성화
- 오류 응답과 로그의 `X-Request-Id`: 장애 요청 추적
