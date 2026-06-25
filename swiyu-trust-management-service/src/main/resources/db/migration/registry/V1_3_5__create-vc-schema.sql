CREATE TABLE vc_schema
(
    id               uuid         NOT NULL,
    file             text         NOT NULL,
    path             text         NOT NULL,
    status           varchar(255) NOT NULL,

    created_by       varchar(255) NOT NULL,
    created_at       timestamp    NOT NULL,
    last_modified_by varchar(255) NOT NULL,
    last_modified_at timestamp    NOT NULL,

    CONSTRAINT uq_vc_schema_path UNIQUE (path),
    PRIMARY KEY (id)
);