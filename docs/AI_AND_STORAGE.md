# AI 및 파일 저장소 운영 기준

## Supabase Storage

- `pawfit` bucket은 private으로 운영합니다.
- 앱과 웹은 Supabase service role key를 받지 않습니다.
- 업로드·삭제·signed URL 발급은 Spring Boot API만 수행합니다.
- 허용 형식은 JPEG, PNG, WebP, PDF이며 기본 최대 크기는 10 MiB입니다.
- 업로드 후 DB transaction이 rollback되면 업로드 파일을 보상 삭제합니다.
- 기존 파일 삭제는 DB transaction commit 후 실행합니다.
- `supabase/setup-storage.sql`을 Supabase SQL Editor에서 한 번 실행합니다.

### 권장 경로

```text
vaccinations/{userId}/{dogId}/{uuid}.{extension}
profiles/{userId}/{uuid}.{extension}
business/{businessId}/{uuid}.{extension}
```

파일 경로는 권한을 대신하지 않습니다. signed URL을 발급하기 전에 요청 사용자가
해당 DB 레코드의 소유자 또는 허용된 운영자인지 application 계층에서 확인합니다.

## AI provider

`AI_PROVIDER`로 구현체를 선택합니다.

- 운영 기본값 `GEMINI`: `GEMINI_API_KEY`, `GEMINI_MODEL=gemini-3.8-flash`
- `WORKERS_AI`: `CLOUDFLARE_ACCOUNT_ID`, `CLOUDFLARE_API_TOKEN`, text/vision model

PawFit은 예방접종 이미지·PDF 분석과 구조화 JSON 응답이 핵심이므로 운영에서는
Gemini 3.8 Flash를 사용합니다. Workers AI는 대체 provider로만 유지하며 Render에는
Cloudflare Secret을 기본 등록하지 않습니다.

공통 설정:

- `AI_TIMEOUT`: 외부 요청 제한 시간
- `AI_MAX_RETRIES`: 최초 요청 이후 최대 재시도 횟수
- `AI_RETRY_DELAY`: 지수 backoff의 시작 지연

HTTP 429와 5xx, 네트워크 오류만 재시도합니다. 예방접종 OCR이 끝까지 실패하면 예외로
사용자 요청을 실패시키지 않고 `NEEDS_MANUAL_REVIEW` 상태를 반환합니다.

## 예방접종 상태 분리

AI 분석과 운영자 판단은 별도 상태입니다.

```text
AI: ANALYZED | NEEDS_MANUAL_REVIEW
운영자: PENDING | APPROVED | REJECTED | NEEDS_MORE_INFORMATION
```

AI가 높은 confidence를 반환해도 운영자 상태를 자동으로 `APPROVED`로 바꾸지 않습니다.
현재 단계에서는 사용자가 개인정보를 마스킹한 문서만 업로드한다는 전제를 사용합니다.
