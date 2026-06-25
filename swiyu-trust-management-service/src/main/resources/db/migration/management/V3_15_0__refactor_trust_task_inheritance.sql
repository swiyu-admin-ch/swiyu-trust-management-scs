-- Phase 1: Refactor trust_onboarding_task into a JPA JOINED inheritance hierarchy
-- Base table: trust_task
-- Subtables: trust_onboarding_task (type-specific), trust_add_did_task (new)

-- Step 1: Rename trust_onboarding_task to trust_task (base table)
ALTER TABLE trust_onboarding_task RENAME TO trust_task;

-- Step 2: Add discriminator column
ALTER TABLE trust_task ADD COLUMN task_type varchar(31) NOT NULL DEFAULT 'ONBOARDING';

-- Step 3: Create trust_onboarding_task subtable with type-specific fields
CREATE TABLE trust_onboarding_task (
    id uuid PRIMARY KEY REFERENCES trust_task(id),
    trust_onboarding_submission_id uuid NOT NULL
);

-- Step 4: Migrate existing data
INSERT INTO trust_onboarding_task (id, trust_onboarding_submission_id)
    SELECT id, trust_onboarding_submission_id FROM trust_task;

-- Step 5: Drop the submission id column from the base table
ALTER TABLE trust_task DROP COLUMN trust_onboarding_submission_id;

-- Step 6: Make partner_id nullable (add-DID tasks may not have a known partner)
ALTER TABLE trust_task ALTER COLUMN partner_id DROP NOT NULL;

-- Step 7: Create trust_add_did_task subtable
CREATE TABLE trust_add_did_task (
    id uuid PRIMARY KEY REFERENCES trust_task(id),
    trust_add_did_submission_id uuid NOT NULL,
    permission_did TEXT NOT NULL
);

-- Step 8: Update domain_event_log FK to reference trust_task
ALTER TABLE domain_event_log DROP CONSTRAINT IF EXISTS domain_event_log_trust_onboarding_task_id_fkey;
ALTER TABLE domain_event_log RENAME COLUMN trust_onboarding_task_id TO trust_task_id;

-- Nullify orphaned references that no longer have a corresponding trust_task row
UPDATE domain_event_log
SET trust_task_id = NULL
WHERE trust_task_id IS NOT NULL
  AND trust_task_id NOT IN (SELECT id FROM trust_task);

ALTER TABLE domain_event_log ADD CONSTRAINT domain_event_log_trust_task_id_fkey
    FOREIGN KEY (trust_task_id) REFERENCES trust_task(id);
