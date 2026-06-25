CREATE TABLE published_statement
(
    id     uuid NOT NULL,
    status VARCHAR(20),
    serialized text  NULL,
    data jsonb NULL,
    type    varchar(100),
    PRIMARY KEY (id)
);

CREATE INDEX idx_ts_identity_v2_search_by_subject ON published_statement USING
    HASH ((data -> 'sub')) WHERE type = 'IDENTITY_TRUST_STATEMENT_V2';

CREATE INDEX idx_ts_identity_v2_search_by_subject_and_time ON published_statement USING
    BTREE ((data -> 'sub'), ((data -> 'exp')::bigint), ((data -> 'nbf')::bigint))
    WHERE type = 'IDENTITY_TRUST_STATEMENT_V2';
