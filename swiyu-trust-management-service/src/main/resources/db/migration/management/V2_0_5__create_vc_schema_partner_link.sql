CREATE TABLE vc_schema_partner_link
(
    id                      uuid         NOT NULL,
    vc_schema_id            uuid         NOT NULL,
    vc_schema_submission_id uuid         NOT NULL,
    partner_id              uuid         NOT NULL,

    created_by              varchar(255) NOT NULL,
    created_at              timestamp    NOT NULL,
    last_modified_by        varchar(255) NOT NULL,
    last_modified_at        timestamp    NOT NULL,

    PRIMARY KEY (id)
);
