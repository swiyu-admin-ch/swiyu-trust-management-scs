CREATE TABLE trust_onboarding_task
(
    id                             uuid         NOT NULL,
    version                        bigint       NOT NULL,
    partner_id                     uuid         NOT NULL,
    partner_name_de                varchar(255) NOT NULL,
    partner_name_fr                varchar(255) NOT NULL,
    partner_name_it                varchar(255) NOT NULL,
    partner_name_en                varchar(255) NOT NULL,
    partner_name_rm                varchar(255) NOT NULL,
    trust_onboarding_submission_id uuid         NOT NULL,
    due_date                       date         NOT NULL,
    submission_date                date         NOT NULL,
    status                         varchar(255) NOT NULL,
    assignee                       varchar(255),

    created_by                     varchar(255) NOT NULL,
    created_at                     timestamp    NOT NULL,
    last_modified_by               varchar(255) NOT NULL,
    last_modified_at               timestamp    NOT NULL,

    PRIMARY KEY (id)
);