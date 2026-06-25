DELETE FROM history_entry
WHERE trust_statement_id IN (
    SELECT id FROM trust_statement WHERE status = 'PREPARATION'
);

DELETE FROM ts_metadata_v1
WHERE trust_statement_id IN (
    SELECT id FROM trust_statement WHERE status = 'PREPARATION'
);

DELETE FROM trust_statement WHERE status = 'PREPARATION';