-- The status list publication looks up the revoked indices of a list by (status_list_metadata_id, status) and reads
-- only status_list_index. The covering index answers that query from the index alone, so the statement rows - and the
-- jsonb details stored in them - are never read. It also serves lookups by status_list_metadata_id alone, which makes
-- the single column index redundant; the foreign key stays indexed through uq_trust_statement_status_list_id_status_list_index.
-- All statements are idempotent because Flyway runs them once.
CREATE INDEX IF NOT EXISTS idx_trust_statement_status_list_lookup
    ON trust_statement (status_list_metadata_id, status)
    INCLUDE (status_list_index);

DROP INDEX IF EXISTS idx_trust_statement_status_list_id;
