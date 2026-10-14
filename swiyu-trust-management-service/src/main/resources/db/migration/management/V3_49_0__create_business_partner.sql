-- Local projection of the business partners owned by the core business service. The authoritative state stays there
-- and is re-read over its internal API on every sync.
CREATE TABLE business_partner
(
    id                  uuid         NOT NULL,
    partner_name        varchar(255),
    uid                 varchar(15),
    type                varchar(255),
    -- A projection is only as current as its last sync, so "may not be deleted" is the safe assumption until one
    -- has run.
    hard_delete_allowed boolean      NOT NULL DEFAULT FALSE,
    synced_at           timestamp    NOT NULL,
    created_by          varchar(255) NOT NULL,
    created_at          timestamp    NOT NULL,
    last_modified_by    varchar(255) NOT NULL,
    last_modified_at    timestamp    NOT NULL,
    PRIMARY KEY (id)
);
