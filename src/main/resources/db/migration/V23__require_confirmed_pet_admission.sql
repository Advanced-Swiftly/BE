ALTER TABLE place_entry_policy ADD COLUMN details_complete BOOLEAN NOT NULL DEFAULT TRUE;
ALTER TABLE place_entry_policy ADD COLUMN max_weight_exclusive BOOLEAN NOT NULL DEFAULT FALSE;
ALTER TABLE public_facility ADD COLUMN admission_evidence TEXT;

INSERT INTO public_data_source(source_id,source_code,name)
VALUES(1702,'TEAM_RESEARCH','팀 직접 조사') ON CONFLICT DO NOTHING;

-- A visible map place must first have affirmative pet admission evidence.
-- The current approved store version is resolved with the same KST effective date as the API.
CREATE OR REPLACE VIEW place_catalog AS
SELECT CONCAT('STORE:',s.store_id) AS place_id,s.name,s.address,s.region_id,s.category,s.latitude,s.longitude
FROM store s
LEFT JOIN LATERAL (
    SELECT p.conditions_json::jsonb AS conditions
    FROM store_policy p
    WHERE p.store_id=s.store_id
      AND COALESCE(NULLIF(p.conditions_json::jsonb->>'effectiveFrom','')::date,DATE '1970-01-01')
          <= (CURRENT_TIMESTAMP AT TIME ZONE 'Asia/Seoul')::date
    ORDER BY p.version DESC LIMIT 1
) current_policy ON TRUE
LEFT JOIN place_entry_policy legacy ON legacy.place_id=CONCAT('STORE:',s.store_id)
    AND legacy.confirmed_on <= (CURRENT_TIMESTAMP AT TIME ZONE 'Asia/Seoul')::date
WHERE s.verification_status='VERIFIED' AND s.visibility='VISIBLE'
  AND COALESCE((current_policy.conditions->>'petsAllowed')::boolean,legacy.pets_allowed,FALSE)=TRUE
UNION ALL
SELECT CONCAT('PUBLIC:',f.facility_id),f.name,f.address,f.region_id,f.category,f.latitude,f.longitude
FROM public_facility f
JOIN place_entry_policy p ON p.place_id=CONCAT('PUBLIC:',f.facility_id)
WHERE f.visible=TRUE AND p.pets_allowed=TRUE
  AND p.confirmed_on <= (CURRENT_TIMESTAMP AT TIME ZONE 'Asia/Seoul')::date;
