ALTER TABLE trust_statement
    ADD COLUMN IF NOT EXISTS details JSONB;