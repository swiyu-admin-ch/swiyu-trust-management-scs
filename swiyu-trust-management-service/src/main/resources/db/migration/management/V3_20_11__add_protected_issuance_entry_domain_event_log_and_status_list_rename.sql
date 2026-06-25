ALTER TABLE domain_event_log ADD COLUMN IF NOT EXISTS protected_issuance_entry_id uuid;

ALTER TABLE status_list RENAME TO status_list_metadata;

ALTER TABLE trust_statement
    RENAME COLUMN status_list_id TO status_list_metadata_id;

ALTER TABLE status_list_metadata
    ADD COLUMN next_free_index INTEGER NOT NULL DEFAULT 0;
