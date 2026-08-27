-- task_type is no longer a JPA discriminator (ONBOARDING/ADD_DID); it now holds the trust task type
-- directly: for onboarding tasks that is the submission subtype (REGISTRATION/PROFILE_CHANGE/RENEWAL),
-- for add-DID tasks it stays ADD_DID. Remap existing rows that still carry the legacy 'ONBOARDING'
-- discriminator value to REGISTRATION (best-effort default; the precise subtype is only known for
-- submissions created after this change).
UPDATE trust_task SET task_type = 'REGISTRATION' WHERE task_type = 'ONBOARDING';
