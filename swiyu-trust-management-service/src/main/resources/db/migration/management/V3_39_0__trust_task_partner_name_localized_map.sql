-- Replace the legacy five-column partner name on trust_task with a single localized JSONB map,
-- aligning with the core business service entity_name representation (mandatory "default" key + BCP-47 locale keys).

ALTER TABLE trust_task ADD COLUMN partner_name jsonb;

UPDATE trust_task SET
    partner_name = jsonb_strip_nulls(jsonb_build_object(
        'default', COALESCE(NULLIF(partner_name_de, ''), NULLIF(partner_name_en, ''), NULLIF(partner_name_fr, ''), NULLIF(partner_name_it, ''), NULLIF(partner_name_rm, ''), 'Unknown'),
        'de-CH', NULLIF(partner_name_de, ''),
        'fr-CH', NULLIF(partner_name_fr, ''),
        'it-CH', NULLIF(partner_name_it, ''),
        'en', NULLIF(partner_name_en, ''),
        'rm-CH', NULLIF(partner_name_rm, '')
    ))
WHERE partner_name IS NULL;

ALTER TABLE trust_task ALTER COLUMN partner_name SET NOT NULL;

ALTER TABLE trust_task
    DROP COLUMN partner_name_de,
    DROP COLUMN partner_name_fr,
    DROP COLUMN partner_name_it,
    DROP COLUMN partner_name_en,
    DROP COLUMN partner_name_rm;
