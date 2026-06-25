CREATE TABLE non_compliant_actor
(
    id                          uuid         NOT NULL,
    version                     bigint       NOT NULL,
    did                         text         NOT NULL UNIQUE,
    flagged_as_non_compliant_at timestamp    NOT NULL,
    reason_de                   text,
    reason_fr                   text,
    reason_it                   text,
    reason_en                   text,
    reason_rm                   text,
    created_by                  varchar(255) NOT NULL,
    created_at                  timestamp    NOT NULL,
    last_modified_by            varchar(255) NOT NULL,
    last_modified_at            timestamp    NOT NULL,
    PRIMARY KEY (id)
);
CREATE EXTENSION IF NOT EXISTS pg_trgm;
CREATE INDEX IF NOT EXISTS idx_nca_did_trgm_ci ON non_compliant_actor USING GIN (lower (did) gin_trgm_ops);

ALTER TABLE domain_event_log DROP CONSTRAINT IF EXISTS domain_event_log_trust_onboarding_task_id_fkey;
ALTER TABLE domain_event_log ADD COLUMN IF NOT EXISTS non_compliant_actor_id uuid;