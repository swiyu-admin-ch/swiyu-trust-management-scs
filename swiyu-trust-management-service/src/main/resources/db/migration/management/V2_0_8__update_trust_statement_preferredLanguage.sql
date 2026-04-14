-- due to the migration the trust statement invalid preferredLanguage where migrated as well which are fixes with this
UPDATE trust_statement
SET details = jsonb_set(details, '{preferredLanguage}', '"de-CH"', false)
WHERE details->>'preferredLanguage' = 'DE_CH';

UPDATE trust_statement
SET details = jsonb_set(details, '{preferredLanguage}', '"fr-CH"', false)
WHERE details->>'preferredLanguage' = 'FR_CH';

UPDATE trust_statement
SET details = jsonb_set(details, '{preferredLanguage}', '"it-CH"', false)
WHERE details->>'preferredLanguage' = 'IT_CH';

UPDATE trust_statement
SET details = jsonb_set(details, '{preferredLanguage}', '"en"', false)
WHERE details->>'preferredLanguage' = 'EN';

-- replace any other invalid preferredLanguage with de-CH
UPDATE trust_statement
SET details = jsonb_set(details, '{preferredLanguage}', '"de-CH"', false)
WHERE details->>'preferredLanguage' not in ('de-CH', 'fr-CH', 'it-CH', 'en');