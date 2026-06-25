CREATE TABLE protected_issuance_entry
(
    id               uuid         NOT NULL,
    protected_at     timestamp    NOT NULL,
    vct              text         NOT NULL UNIQUE,

    created_by       varchar(255) NOT NULL,
    created_at       timestamp    NOT NULL,
    last_modified_by varchar(255) NOT NULL,
    last_modified_at timestamp    NOT NULL,
    PRIMARY KEY (id)
);