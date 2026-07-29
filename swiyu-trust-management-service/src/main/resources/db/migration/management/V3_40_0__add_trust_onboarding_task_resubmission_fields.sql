-- dueAt is no longer mandatory once a task is awaiting more information from the partner
ALTER TABLE trust_task ALTER COLUMN due_at DROP NOT NULL;

ALTER TABLE trust_onboarding_task ADD COLUMN rejection_enforced_at timestamp;
ALTER TABLE trust_onboarding_task ADD COLUMN times_resubmitted integer NOT NULL DEFAULT 0;
