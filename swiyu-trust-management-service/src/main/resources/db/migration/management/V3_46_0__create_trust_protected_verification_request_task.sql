CREATE TABLE trust_protected_verification_request_task (
    id uuid PRIMARY KEY REFERENCES trust_task(id),
    protected_verification_submission_id uuid NOT NULL,
    zas_data_opened_at timestamp
);
