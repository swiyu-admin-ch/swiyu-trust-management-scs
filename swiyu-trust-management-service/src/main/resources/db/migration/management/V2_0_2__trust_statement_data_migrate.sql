DO
$$
    DECLARE
        record       RECORD;
        json_details JSONB;
    BEGIN
        FOR record IN
            SELECT trust_statement_id,
                   preferred_language,
                   org_name_en,
                   org_name_de_ch,
                   org_name_fr_ch,
                   org_name_it_ch,
                   org_name_rm_ch,
                   logo_uri_en,
                   logo_uri_de_ch,
                   logo_uri_fr_ch,
                   logo_uri_it_ch,
                   logo_uri_rm_ch
            FROM ts_metadata_v1
            LOOP
                json_details := jsonb_build_object(
                        'type', 'TRUST_STATEMENT_METADATA_V1',
                        'preferredLanguage', record.preferred_language,
                        'orgName', jsonb_strip_nulls(jsonb_build_object(
                                'en', record.org_name_en,
                                'de-CH', record.org_name_de_ch,
                                'fr-CH', record.org_name_fr_ch,
                                'it-CH', record.org_name_it_ch,
                                'rm-CH', record.org_name_rm_ch
                                                     )),
                        'logoUri', jsonb_strip_nulls(jsonb_build_object(
                                'en', record.logo_uri_en,
                                'de-CH', record.logo_uri_de_ch,
                                'fr-CH', record.logo_uri_fr_ch,
                                'it-CH', record.logo_uri_it_ch,
                                'rm-CH', record.logo_uri_rm_ch
                                                     ))
                                );

                UPDATE trust_statement
                SET details          = json_details,
                    last_modified_by = 'migration_script',
                    last_modified_at = NOW()
                WHERE id = record.trust_statement_id
                    AND details IS NULL
                   OR NOT EXISTS (SELECT 1
                                  FROM jsonb_object_keys(details) keys
                                  WHERE keys = 'preferredLanguage')
                   OR details ->> 'preferredLanguage' IS NULL
                   OR details ->> 'preferredLanguage' = '';

            END LOOP;
    END
$$;