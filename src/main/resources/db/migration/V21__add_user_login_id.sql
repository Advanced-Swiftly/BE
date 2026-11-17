-- 전화번호로 가입한 기존 계정은 그대로 유지하고 이메일 아이디를 선택적으로 추가한다.
ALTER TABLE app_user ADD COLUMN login_id VARCHAR(254);
ALTER TABLE app_user ADD CONSTRAINT uq_app_user_login_id UNIQUE (login_id);
ALTER TABLE app_user ADD CONSTRAINT chk_app_user_login_id_normalized
    CHECK (login_id IS NULL OR (login_id = lower(trim(login_id)) AND length(login_id) > 0));
