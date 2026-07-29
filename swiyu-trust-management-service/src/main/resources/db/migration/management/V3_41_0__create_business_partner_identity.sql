CREATE TABLE business_partner_identity
(
    id                                   uuid         NOT NULL,
    partner_name                         jsonb        NOT NULL,
    last_activated                       timestamp,
    uid                                  varchar(15),
    is_registered_in_commercial_register boolean      NOT NULL,
    corresponding_language               varchar(255),
    status                               varchar(255) NOT NULL,
    is_state_actor                       boolean      NOT NULL,
    trusted_identifier                   jsonb        NOT NULL,
    valid_until                          timestamp,
    last_issuance_at                     timestamp,
    version                              bigint       NOT NULL,
    created_by                           varchar(255) NOT NULL,
    created_at                           timestamp    NOT NULL,
    last_modified_by                     varchar(255) NOT NULL,
    last_modified_at                     timestamp    NOT NULL,
    PRIMARY KEY (id)
);
