-- Naming convention for INDEX:
-- https://stackoverflow.com/questions/4107915/postgresql-default-constraint-names/4108266#4108266
CREATE TABLE trust_statement
(
    id                         uuid         NOT NULL,
    type                       varchar(255) NOT NULL,
    status                     varchar(50)  NOT NULL,
    subject                    text         NOT NULL,
    valid_from                 timestamp    NOT NULL,
    valid_until                timestamp    NOT NULL,

    trust_registry_entry_id    uuid         NULL,
    trust_issuer_credential_id uuid         NULL,

    created_by                 varchar(255) NOT NULL,
    created_at                 timestamp    NOT NULL,
    last_modified_by           varchar(255) NOT NULL,
    last_modified_at           timestamp    NOT NULL,

    PRIMARY KEY (id)
);

CREATE TABLE history_entry
(
    id                 uuid         NOT NULL,
    trust_statement_id uuid         NOT NULL,
    severity           varchar(50)  NOT NULL,
    details            text         NOT NULL,

    created_by         varchar(255) NOT NULL,
    created_at         timestamp    NOT NULL,
    last_modified_by   varchar(255) NOT NULL,
    last_modified_at   timestamp    NOT NULL,
    PRIMARY KEY (id),
    CONSTRAINT history_entry_trust_statement_id_fkey
        FOREIGN KEY (trust_statement_id)
            REFERENCES trust_statement (id)
            ON DELETE RESTRICT

);

CREATE TABLE ts_metadata_v1
(
    trust_statement_id uuid       NOT NULL,
    preferred_language varchar(5) NOT NULL,

    org_name_en        text       NULL,
    org_name_de_ch     text       NULL,
    org_name_fr_ch     text       NULL,
    org_name_it_ch     text       NULL,
    org_name_rm_ch     text       NULL,

    logo_uri_en        text       NULL,
    logo_uri_de_ch     text       NULL,
    logo_uri_fr_ch     text       NULL,
    logo_uri_it_ch     text       NULL,
    logo_uri_rm_ch     text       NULL,


    PRIMARY KEY (trust_statement_id),
    CONSTRAINT ts_metadata_v1_trust_statement_id_fkey
        FOREIGN KEY (trust_statement_id)
            REFERENCES trust_statement (id)
            ON DELETE RESTRICT
);
