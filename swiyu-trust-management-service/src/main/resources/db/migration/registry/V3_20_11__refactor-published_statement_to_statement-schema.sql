-- Can be dropped as we have this table not in active usage
DROP TABLE published_statement;

CREATE TABLE statement
(
    id                      uuid         NOT NULL,
    is_soft_deleted         boolean,
    is_active_in_statuslist boolean,
    serialized              text NULL,
    data                    jsonb NULL,
    type                    varchar(100),

    created_by              varchar(255) NOT NULL,
    created_at              timestamp    NOT NULL,
    last_modified_by        varchar(255) NOT NULL,
    last_modified_at        timestamp    NOT NULL,
    PRIMARY KEY (id)
);

CREATE INDEX statement_type_is_active_in_statuslist_is_soft_deleted_idx
    ON statement (
                  type,
                  is_active_in_statuslist,
                  is_soft_deleted
    );

CREATE INDEX statement_type_is_soft_deleted_data_sub_idx
    ON statement (type, is_soft_deleted, (data -> 'sub'));

CREATE INDEX statement_data_sub_idx
    ON statement (type, is_soft_deleted, is_active_in_statuslist, (data -> 'sub'));

CREATE INDEX statement_data_sub_exp_nbf_idx
    ON statement USING BTREE (
        type,
        is_soft_deleted,
        is_active_in_statuslist,
        (data -> 'sub'),
        ((data -> 'exp')::bigint),
        ((data -> 'nbf')::bigint)
    );