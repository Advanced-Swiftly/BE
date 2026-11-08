-- V14까지 적용한 테스트 DB에서 실행합니다.
BEGIN;

DO $$
DECLARE
    owner_id BIGINT;
    breed_key BIGINT;
    deleted_dog_id BIGINT;
    remaining_dog_id BIGINT;
    visit_key BIGINT;
BEGIN
    INSERT INTO app_user(phone_number, password_hash, name, role, status)
    VALUES ('01000003400', 'test', '삭제 검증', 'GUARDIAN', 'ACTIVE')
    RETURNING user_id INTO owner_id;

    SELECT breed_id INTO STRICT breed_key FROM breed ORDER BY breed_id LIMIT 1;

    INSERT INTO dog(user_id, breed_id, name) VALUES (owner_id, breed_key, '삭제할 반려견')
    RETURNING dog_id INTO deleted_dog_id;
    INSERT INTO dog(user_id, breed_id, name) VALUES (owner_id, breed_key, '남은 반려견')
    RETURNING dog_id INTO remaining_dog_id;
    INSERT INTO visit(user_id, store_id, qr_token_hash, visited_at)
    VALUES (owner_id, 100, repeat('0', 64), CURRENT_TIMESTAMP)
    RETURNING visit_id INTO visit_key;
    INSERT INTO visit_dog(visit_id, dog_id)
    VALUES (visit_key, deleted_dog_id), (visit_key, remaining_dog_id);

    DELETE FROM dog WHERE dog_id = deleted_dog_id;

    IF EXISTS (SELECT 1 FROM dog WHERE dog_id = deleted_dog_id)
        OR EXISTS (SELECT 1 FROM visit_dog WHERE dog_id = deleted_dog_id)
        OR NOT EXISTS (SELECT 1 FROM visit WHERE visit_id = visit_key)
        OR NOT EXISTS (SELECT 1 FROM visit_dog WHERE visit_id = visit_key AND dog_id = remaining_dog_id)
    THEN
        RAISE EXCEPTION '삭제 이후 방문과 동반견 연결 상태가 올바르지 않습니다';
    END IF;
END
$$;

ROLLBACK;
