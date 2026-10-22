-- One row per business partner for the partner view in the management UI. The join is a full outer one on purpose: a
-- partner may be known to only one of the two sides, and both belong in a list used to find a partner and act on it.
CREATE VIEW business_partner_view AS
SELECT COALESCE(bp.id, bpi.id)                     AS business_partner_id,
       COALESCE(bp.partner_name, bpi.partner_name ->> 'default')
                                                   AS partner_name,
       COALESCE(bp.uid, bpi.uid)                   AS uid,
       bp.type                                     AS type,
       COALESCE(bp.hard_delete_allowed, FALSE)     AS hard_delete_allowed,
       bp.synced_at                                AS synced_at,
       bpi.status                                  AS status,
       bpi.valid_until                             AS valid_until,
       bpi.last_activated                          AS last_activated,
       bpi.last_issuance_at                        AS last_issuance_at,
       bpi.is_state_actor                          AS is_state_actor,
       bpi.is_registered_in_commercial_register    AS is_registered_in_commercial_register,
       bpi.corresponding_language                  AS corresponding_language
FROM business_partner bp
         FULL OUTER JOIN business_partner_identity bpi ON bpi.id = bp.id;
