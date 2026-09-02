package ch.admin.bj.swiyu.trust.management.modules.management.domain.task;

import java.util.Set;
import lombok.experimental.UtilityClass;

@UtilityClass
public class TaskStatusValidator {

    public static void validateNewStatus(TaskStatus currentStatus, TaskStatus newStatus) {
        if (!isValidNewStatus(currentStatus, newStatus)) {
            throw new IllegalStateException("Cannot change status from " + currentStatus + " to " + newStatus);
        }
    }

    public static boolean isValidNewStatus(TaskStatus currentStatus, TaskStatus newStatus) {
        return getPossibleNewStatus(currentStatus).contains(newStatus);
    }

    private static Set<TaskStatus> getPossibleNewStatus(TaskStatus currentStatus) {
        return switch (currentStatus) {
            case OPENED -> Set.of(TaskStatus.INFORMATION_REQUESTED, TaskStatus.ACCEPTED, TaskStatus.REJECTED);
            case INFORMATION_REQUESTED -> Set.of(TaskStatus.RESUBMITTED, TaskStatus.ACCEPTED, TaskStatus.REJECTED);
            case RESUBMITTED -> Set.of(TaskStatus.INFORMATION_REQUESTED, TaskStatus.ACCEPTED, TaskStatus.REJECTED);
            case ACCEPTED, REJECTED -> Set.of();
        };
    }
}
