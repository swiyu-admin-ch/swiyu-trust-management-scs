ALTER TABLE trust_task RENAME TO task;
ALTER TABLE trust_protected_verification_request_task RENAME TO protected_verification_request_task;
ALTER TABLE domain_event_log RENAME COLUMN trust_task_id TO task_id;
ALTER TABLE domain_event_log RENAME COLUMN business_partner_identity_id TO business_partner_id;
UPDATE domain_event_log SET event_type = 'TASK_NOTE_ADDED' WHERE event_type = 'TRUST_ONBOARDING_TASK_NOTE_ADDED';
UPDATE domain_event_log SET event_type = 'TASK_ASSIGNED' WHERE event_type = 'TRUST_ONBOARDING_TASK_ASSIGNED';