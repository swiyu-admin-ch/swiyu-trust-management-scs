package ch.admin.bj.swiyu.trust.management.modules.management.domain.task;

import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.api.Test;

class TaskStatusValidatorTest {

    @Test
    public void testValidateNewStatus() {
        assertThrows(IllegalStateException.class, () ->
            TaskStatusValidator.validateNewStatus(TaskStatus.ACCEPTED, TaskStatus.OPENED)
        );
        assertThrows(IllegalStateException.class, () ->
            TaskStatusValidator.validateNewStatus(TaskStatus.REJECTED, TaskStatus.OPENED)
        );
        TaskStatusValidator.validateNewStatus(TaskStatus.OPENED, TaskStatus.INFORMATION_REQUESTED);
        TaskStatusValidator.validateNewStatus(TaskStatus.OPENED, TaskStatus.REJECTED);
        TaskStatusValidator.validateNewStatus(TaskStatus.INFORMATION_REQUESTED, TaskStatus.RESUBMITTED);
        TaskStatusValidator.validateNewStatus(TaskStatus.INFORMATION_REQUESTED, TaskStatus.REJECTED);
        TaskStatusValidator.validateNewStatus(TaskStatus.RESUBMITTED, TaskStatus.INFORMATION_REQUESTED);
        TaskStatusValidator.validateNewStatus(TaskStatus.RESUBMITTED, TaskStatus.ACCEPTED);
        TaskStatusValidator.validateNewStatus(TaskStatus.RESUBMITTED, TaskStatus.REJECTED);
        assertThrows(IllegalStateException.class, () ->
            TaskStatusValidator.validateNewStatus(TaskStatus.INFORMATION_REQUESTED, TaskStatus.OPENED)
        );
    }
}
