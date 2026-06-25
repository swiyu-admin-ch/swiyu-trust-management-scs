-- Recreate fk_vc_entity_datastore_entity_base with cascading delete
ALTER TABLE vc_entity
    DROP CONSTRAINT fk_vc_entity_datastore_entity_base;

ALTER TABLE vc_entity
    ADD CONSTRAINT fk_vc_entity_datastore_entity_base
        FOREIGN KEY (base_id)
            REFERENCES datastore_entity(id)
            ON DELETE CASCADE;

-- delete all MetadataV1 entries
DELETE FROM datastore_entity WHERE id IN (
        SELECT base_id
            FROM vc_entity
            WHERE vc_type = 'TrustStatementMetadataV1'
);
