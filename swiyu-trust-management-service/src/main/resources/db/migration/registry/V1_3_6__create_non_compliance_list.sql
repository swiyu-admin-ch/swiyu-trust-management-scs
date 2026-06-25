CREATE TABLE non_compliance_list
(
    id               uuid         NOT NULL,
    version          bigint       NOT NULL,
    published_at     timestamp    NOT NULL,
    payload          text         NOT NULL,
    created_by       varchar(255) NOT NULL,
    created_at       timestamp    NOT NULL,
    last_modified_by varchar(255) NOT NULL,
    last_modified_at timestamp    NOT NULL,
    PRIMARY KEY (id)
);
CREATE INDEX IF NOT EXISTS idx_non_compliance_list_published_at ON non_compliance_list (published_at);