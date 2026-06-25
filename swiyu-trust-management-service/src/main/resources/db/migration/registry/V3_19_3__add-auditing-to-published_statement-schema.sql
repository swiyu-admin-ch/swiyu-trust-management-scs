ALTER TABLE published_statement
    ADD COLUMN created_by       varchar(255) NULL,
    ADD COLUMN created_at       timestamp    NULL,
    ADD COLUMN last_modified_by varchar(255) NULL,
    ADD COLUMN last_modified_at timestamp    NULL;

UPDATE published_statement
SET
  created_by = 'system',
  created_at = now(),
  last_modified_by = 'system',
  last_modified_at = now()
WHERE created_at IS NULL;

ALTER TABLE published_statement
    ALTER COLUMN created_by        SET NOT NULL,
    ALTER COLUMN created_at        SET NOT NULL,
    ALTER COLUMN last_modified_by  SET NOT NULL,
    ALTER COLUMN last_modified_at  SET NOT NULL;

