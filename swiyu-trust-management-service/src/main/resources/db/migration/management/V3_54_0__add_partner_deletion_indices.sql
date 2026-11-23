-- The hard delete of a business partner looks its rows up by these columns; without an index every step scans the
-- whole table. All statements are idempotent because Flyway runs them once.
CREATE INDEX IF NOT EXISTS idx_trust_statement_partner_id ON trust_statement (partner_id);
CREATE INDEX IF NOT EXISTS idx_task_partner_id ON task (partner_id);
CREATE INDEX IF NOT EXISTS idx_domain_event_log_business_partner_id ON domain_event_log (business_partner_id);
CREATE INDEX IF NOT EXISTS idx_vc_schema_partner_link_partner_id ON vc_schema_partner_link (partner_id);
CREATE INDEX IF NOT EXISTS idx_non_compliant_actor_business_partner_id ON non_compliant_actor (business_partner_id);
