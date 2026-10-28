-- EID-6985: read-performance quickfix — replace the statement/vc_entity indexes with
-- expression indexes that match the actual read-query predicates.
--
-- The V2 statement search filters on (data->>'typ') text, (data->>'sub') text,
-- (data->>'jti')::uuid and (data->>'exp'|'nbf')::bigint casts; the previous indexes
-- used the plain `type` column and jsonb extraction (data -> 'sub'), which do not
-- match the query expressions and are therefore unusable for them.
--
-- Can be dropped as no query filters on the `type` column anymore.
DROP INDEX IF EXISTS statement_type_is_soft_deleted_data_sub_idx;
DROP INDEX IF EXISTS statement_data_sub_idx;
DROP INDEX IF EXISTS statement_data_sub_exp_nbf_idx;
DROP INDEX IF EXISTS statement_type_is_active_in_statuslist_is_soft_deleted_idx;

-- Leading key is the JSON typ extracted as text: searches filter on data ->> 'typ',
-- not type, so the index can support typ and status predicates.
CREATE INDEX statement_typ_is_active_in_statuslist_is_soft_deleted_idx
    ON statement ((data ->> 'typ'), is_active_in_statuslist, is_soft_deleted);

-- Typ + soft-deleted + subject searches, including those without an
-- active-in-statuslist filter.
CREATE INDEX statement_typ_is_soft_deleted_sub_idx
    ON statement ((data ->> 'typ'), is_soft_deleted, (data ->> 'sub'));

-- Active subject searches and their validity-range predicates; the 4-column
-- equality prefix is contained in this index.
CREATE INDEX statement_typ_is_soft_deleted_active_sub_exp_nbf_idx
    ON statement (
        (data ->> 'typ'),
        is_soft_deleted,
        is_active_in_statuslist,
        (data ->> 'sub'),
        ((data ->> 'exp')::bigint),
        ((data ->> 'nbf')::bigint)
    );

-- Support V2 JTI lookups using JSON typ, matching the unchanged queries.
CREATE INDEX statement_typ_is_soft_deleted_jti_idx
    ON statement ((data ->> 'typ'), is_soft_deleted, ((data ->> 'jti')::uuid));

-- Support V1 subject lookups without scanning all credentials.
CREATE INDEX vc_entity_sub_idx
    ON vc_entity ((vc_payload ->> 'sub'));

-- Support V1 lookups by the schema a credential permits issuing.
CREATE INDEX vc_entity_can_issue_idx
    ON vc_entity ((vc_payload ->> 'canIssue'));

-- Support V1 lookups by the schema a credential permits verifying.
CREATE INDEX vc_entity_can_verify_idx
    ON vc_entity ((vc_payload ->> 'canVerify'));
