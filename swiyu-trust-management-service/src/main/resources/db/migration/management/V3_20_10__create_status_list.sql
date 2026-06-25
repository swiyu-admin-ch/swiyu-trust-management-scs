CREATE TABLE status_list
(
    id                  uuid         NOT NULL,
    max_size            int          NOT NULL,
    status              varchar(20)  NOT NULL,
    last_published_at   timestamp,
    status_registry_url text,

    created_by          varchar(255) NOT NULL,
    created_at          timestamp    NOT NULL,
    last_modified_by    varchar(255) NOT NULL,
    last_modified_at    timestamp    NOT NULL,
    PRIMARY KEY (id)
);

ALTER TABLE trust_statement
    ADD COLUMN status_list_id uuid,
    ADD COLUMN status_list_index integer,
    ADD CONSTRAINT fk_trust_statement_status_list_id
        FOREIGN KEY (status_list_id)
            REFERENCES status_list (id)
        ON
DELETE
RESTRICT,
    ADD CONSTRAINT uq_trust_statement_status_list_id_status_list_index
        UNIQUE (status_list_id, status_list_index);

CREATE INDEX idx_trust_statement_status_list_id
    ON trust_statement (status_list_id);