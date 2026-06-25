CREATE TABLE domain_event_log
(
    id                       uuid         NOT NULL,
    event_type               varchar(255) NOT NULL,
    partner_note             text,
    internal_note            text,
    triggered_at             timestamp    NOT NULL,
    triggered_by             varchar(255) NOT NULL,
    created_by               varchar(255) NOT NULL,
    created_at               timestamp    NOT NULL,
    last_modified_by         varchar(255) NOT NULL,
    last_modified_at         timestamp    NOT NULL,
    trust_onboarding_task_id uuid,
    CONSTRAINT domain_event_log_trust_onboarding_task_id_fkey
        FOREIGN KEY (trust_onboarding_task_id)
            REFERENCES trust_onboarding_task(id),
    PRIMARY KEY (id)
);