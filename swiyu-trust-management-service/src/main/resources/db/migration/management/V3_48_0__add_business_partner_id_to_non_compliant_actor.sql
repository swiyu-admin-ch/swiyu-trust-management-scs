ALTER TABLE non_compliant_actor ADD COLUMN IF NOT EXISTS business_partner_id uuid;
ALTER TABLE non_compliant_actor ALTER COLUMN did DROP NOT NULL;
