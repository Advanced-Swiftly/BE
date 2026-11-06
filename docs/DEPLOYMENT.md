# 배포 설정

## 1. Supabase

1. Free 프로젝트를 생성합니다.
2. Dashboard의 **Connect**에서 **Session pooler**와 **JDBC**를 선택합니다.
3. 포트 5432의 값을 아래 Render 환경 변수에 분리해 저장합니다.
   - `DB_URL`: `jdbc:postgresql://<pooler-host>:5432/postgres?sslmode=require`
   - `DB_USERNAME`: `postgres.<project-ref>`
   - `DB_PASSWORD`: 데이터베이스 비밀번호
4. Storage에 private bucket `pawfit`을 생성합니다.
5. 서비스 역할 키는 백엔드의 `SUPABASE_SERVICE_ROLE_KEY`에만 저장하고 앱이나 Git에는 넣지 않습니다.

Render는 장기 실행 컨테이너이며 Free 환경에서 IPv4 연결이 필요하므로 Supabase Session pooler를 사용합니다. Hikari 최대 연결 수는 기본 5로 제한했습니다.

## 2. Render

1. Render Dashboard에서 **New > Blueprint**를 선택합니다.
2. `develop` 브랜치가 원격에 생성되어 있고 초기 세팅이 포함됐는지 확인합니다.
3. GitHub의 `Advanced-Swiftly/BE` 저장소를 연결합니다.
4. 루트의 `render.yaml`을 적용합니다.
5. `sync: false` 환경 변수를 입력합니다.
6. Web Service의 **Settings > Deploy Hook**에서 hook URL을 생성합니다.

`render.yaml`은 Singapore, Free, Docker, `develop` 브랜치를 사용하고 자동 배포는 꺼 둡니다. GitHub Actions의 테스트가 성공한 경우에만 Deploy Hook을 호출합니다.

## 3. GitHub Actions

Repository **Settings > Secrets and variables > Actions**에 다음 secret을 추가합니다.

- `RENDER_DEPLOY_HOOK_URL`: Render에서 생성한 deploy hook

흐름:

1. PR 및 `main`/`develop` push에서 테스트, JAR 빌드, Docker 이미지 빌드
2. `develop` CI 성공 시 Render deploy hook 호출
3. Render가 새 커밋을 빌드하고 `/actuator/health`로 상태 확인

브랜치 보호 규칙에서 `Backend CI / Test and build`를 필수 검사로 설정하는 것을 권장합니다.

## 4. UptimeRobot

배포 URL의 `https://<service>.onrender.com/actuator/health`를 HTTP(s) monitor로 등록합니다. Free 인스턴스는 유휴 시 중지될 수 있으므로 첫 요청은 느릴 수 있습니다.

## 5. 운영 점검

- `/actuator/health`: Render/UptimeRobot 상태 확인
- `/actuator/info`: 빌드 버전 확인
- `/swagger-ui.html`: API 문서
- 오류 응답과 로그의 `X-Request-Id`: 장애 요청 추적
