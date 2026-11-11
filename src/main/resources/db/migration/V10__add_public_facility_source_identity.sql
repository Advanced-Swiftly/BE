ALTER TABLE public_facility
    ADD COLUMN source_code VARCHAR(40),
    ADD COLUMN external_id VARCHAR(80),
    ADD CONSTRAINT chk_public_facility_source_pair
        CHECK ((source_code IS NULL) = (external_id IS NULL)),
    ADD CONSTRAINT uq_public_facility_source UNIQUE (source_code, external_id);
