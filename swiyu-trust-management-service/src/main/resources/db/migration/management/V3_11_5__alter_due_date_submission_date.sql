-- Rename columns
ALTER TABLE trust_onboarding_task RENAME COLUMN due_date TO due_at;
ALTER TABLE trust_onboarding_task RENAME COLUMN submission_date TO submitted_at;

-- Change data types from date to timestamp (is always valid and time is set to 00:00:00 by default)
ALTER TABLE trust_onboarding_task ALTER COLUMN due_at TYPE timestamp USING due_at::timestamp;
ALTER TABLE trust_onboarding_task ALTER COLUMN submitted_at TYPE timestamp USING submitted_at::timestamp;