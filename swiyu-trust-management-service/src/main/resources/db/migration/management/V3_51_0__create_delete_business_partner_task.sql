-- Subtype table for the delete-business-partner task, following the JOINED inheritance of the other task types.
-- hard_delete_due_at is kept here rather than reusing task.due_at, which is cleared on approve/reject: the moment the
-- deletion was scheduled for has to stay readable after the task is closed.
CREATE TABLE delete_business_partner_task (
    id                  uuid PRIMARY KEY REFERENCES task (id),
    hard_delete_due_at  timestamptz NOT NULL
);

-- The auto-approval job (EID-6994) scans for tasks that have become due, so it reads this column with a status filter.
CREATE INDEX idx_delete_business_partner_task_hard_delete_due_at ON delete_business_partner_task (hard_delete_due_at);
