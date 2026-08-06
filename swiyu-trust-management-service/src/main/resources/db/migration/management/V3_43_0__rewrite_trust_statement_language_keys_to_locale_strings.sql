-- Rewrites trust_statement.details jsonb keys from the old Language enum names (e.g.
-- "DE_CH") to locale strings (e.g. "de-CH"), now that *Details.Language has been replaced
-- by plain String keys. Uses plain text substitution since ':' in JSON only ever follows a
-- key, never a value, so these patterns can only match object keys.
DO $$
DECLARE
    key_rename text[][] := ARRAY[
        ['"DEFAULT":', '"default":'],
        ['"EN_CH":',   '"en-CH":'],
        ['"DE_CH":',   '"de-CH":'],
        ['"FR_CH":',   '"fr-CH":'],
        ['"IT_CH":',   '"it-CH":'],
        ['"RM_CH":',   '"rm-CH":'],
        ['"EN":',      '"en":']
    ];
    pair text[];
BEGIN
    FOREACH pair SLICE 1 IN ARRAY key_rename
    LOOP
        UPDATE trust_statement
        SET details = replace(details::text, pair[1], pair[2])::jsonb
        WHERE type IN (
            'TRUST_STATEMENT_IDENTITY_V2',
            'PUBLIC_STATEMENT_VERIFICATION_QUERY_V2',
            'TRUST_STATEMENT_PROTECTED_ISSUANCE_AUTHORIZATION_V2',
            'TRUST_LIST_STATEMENT_NON_COMPLIANCE_V2'
        )
        AND details::text LIKE '%' || pair[1] || '%';
    END LOOP;
END $$;
