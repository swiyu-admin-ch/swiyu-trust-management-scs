-- A partner must never have more than one pending deletion request. The service checks for one before inserting, but
-- that read happens before the write, so two editors requesting a deletion at the same time both pass the check. This
-- partial unique index makes the database the arbiter; the statuses mirror the pending ones in
-- DeleteBusinessPartnerTaskService. Closed requests (ACCEPTED, REJECTED) are excluded, so a partner can be requested
-- again after a rejection.
CREATE UNIQUE INDEX uq_task_single_pending_delete_business_partner
    ON task (partner_id)
    WHERE task_type = 'DELETE_BUSINESS_PARTNER'
      AND status IN ('OPENED', 'INFORMATION_REQUESTED', 'RESUBMITTED');
