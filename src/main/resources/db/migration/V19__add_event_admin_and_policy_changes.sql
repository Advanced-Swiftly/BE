-- Issue #50 운영자 심사 나머지 API 지원.

-- 이벤트 초안(DRAFT)과 추첨 완료(DRAWN) 상태를 추가한다.
ALTER TABLE stamp_event DROP CONSTRAINT chk_stamp_event_status;
ALTER TABLE stamp_event ADD CONSTRAINT chk_stamp_event_status
    CHECK (status IN ('DRAFT', 'OPEN', 'CLOSED', 'DRAWN'));

-- 운영자가 직접 바꾼 정책도 같은 요청 테이블에 기록한다. (기존 행은 모두 업체 요청)
ALTER TABLE condition_request ADD COLUMN kind VARCHAR(20) NOT NULL DEFAULT 'CONDITION_REQUEST';
ALTER TABLE condition_request ADD CONSTRAINT chk_condition_request_kind
    CHECK (kind IN ('CONDITION_REQUEST', 'OPERATOR_DIRECT'));

-- 예방접종 검토 처리에서 운영자가 남기는 사유. 예방접종 테이블은 건드리지 않고 따로 둔다.
CREATE TABLE vaccination_review (
    record_id       BIGINT       PRIMARY KEY REFERENCES vaccination_record (record_id) ON DELETE CASCADE,
    review_comment  VARCHAR(500),
    reviewed_by     BIGINT,
    created_at      TIMESTAMPTZ  NOT NULL DEFAULT now(),
    updated_at      TIMESTAMPTZ  NOT NULL DEFAULT now()
);
