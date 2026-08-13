CREATE TABLE protected_verification_authorization
(
    id                           uuid         NOT NULL,
    protected_verification_field varchar(255) NOT NULL,
    business_partner_identity_id uuid         NOT NULL,

    created_by                   varchar(255) NOT NULL,
    created_at                   timestamp    NOT NULL,
    last_modified_by             varchar(255) NOT NULL,
    last_modified_at             timestamp    NOT NULL,
    PRIMARY KEY (id)
);

ALTER TABLE domain_event_log ADD COLUMN protected_verification_authorization_id uuid;
