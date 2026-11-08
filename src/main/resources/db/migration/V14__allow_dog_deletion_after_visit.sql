ALTER TABLE visit_dog
    DROP CONSTRAINT visit_dog_dog_id_fkey,
    ADD CONSTRAINT visit_dog_dog_id_fkey
        FOREIGN KEY (dog_id) REFERENCES dog (dog_id) ON DELETE CASCADE;

CREATE INDEX idx_visit_dog_dog_id ON visit_dog (dog_id);
