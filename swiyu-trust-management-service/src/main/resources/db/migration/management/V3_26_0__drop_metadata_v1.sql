DROP TABLE ts_metadata_v1;
DELETE FROM history_entry
    WHERE trust_statement_id IN (
        SELECT id
        FROM trust_statement
        WHERE type = 'TRUST_STATEMENT_METADATA_V1'
    );
DELETE FROM trust_statement WHERE type='TRUST_STATEMENT_METADATA_V1';