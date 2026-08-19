CREATE TABLE protected_issuance_authorization
(
    id                           uuid         NOT NULL,
    business_partner_identity_id uuid         NOT NULL REFERENCES business_partner_identity (id),
    protected_issuance_entry_id  uuid         NOT NULL REFERENCES protected_issuance_entry (id),
    reason                       jsonb,
    created_by                   varchar(255) NOT NULL,
    created_at                   timestamp    NOT NULL,
    last_modified_by             varchar(255) NOT NULL,
    last_modified_at             timestamp    NOT NULL,
    PRIMARY KEY (id)
);

ALTER TABLE trust_statement
    ADD COLUMN protected_issuance_authorization_id uuid;

ALTER TABLE domain_event_log
    ADD COLUMN business_partner_identity_id        uuid,
    ADD COLUMN protected_issuance_authorization_id uuid;

ALTER TABLE protected_issuance_entry
    ADD COLUMN name jsonb;
