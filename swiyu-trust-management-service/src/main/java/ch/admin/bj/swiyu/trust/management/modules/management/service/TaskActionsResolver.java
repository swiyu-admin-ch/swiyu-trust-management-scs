package ch.admin.bj.swiyu.trust.management.modules.management.service;

import static ch.admin.bj.swiyu.trust.management.modules.management.domain.task.TaskStatusValidator.isValidNewStatus;

import ch.admin.bj.swiyu.trust.management.modules.management.api.task.taskaction.TaskActionDto;
import ch.admin.bj.swiyu.trust.management.modules.management.domain.task.ProtectedVerificationRequestTask;
import ch.admin.bj.swiyu.trust.management.modules.management.domain.task.Task;
import ch.admin.bj.swiyu.trust.management.modules.management.domain.task.TaskStatus;
import ch.admin.bj.swiyu.trust.management.modules.management.domain.task.TrustOnboardingTask;
import java.util.HashSet;
import java.util.Objects;
import java.util.Set;
import lombok.experimental.UtilityClass;

@UtilityClass
public class TaskActionsResolver {

    /**
     * Validates the requested action against the same {@link TaskActionsResolver#resolvePossibleActions(Task, String)}
     * result the UI uses to decide which actions to show, so the two never diverge.
     */
    public static void validateActionAllowed(Task task, String triggeredBy, TaskActionDto action) {
        var allowedActions = resolvePossibleActions(task, triggeredBy);
        if (!allowedActions.contains(action)) {
            throw new IllegalArgumentException(
                "Action %s is not allowed for task %s in its current state. Only allowed actions are %s".formatted(
                    action,
                    task.getId(),
                    allowedActions
                )
            );
        }
    }

    /**
     * For a protected verification request, APPROVE/REJECT are only allowed once its ZAS data has been reviewed
     * (see {@link TaskActionsResolver#resolvePossibleActions}) - once that's true, they're just as
     * available from the generic task list row menu as from the dedicated detail page.
     */
    public static Set<TaskActionDto> resolvePossibleActions(Task task, String currentUserFullName) {
        var canRequestMoreInformation = false;
        if (task instanceof TrustOnboardingTask onboardingTask) {
            canRequestMoreInformation = onboardingTask.canRequestMoreInformation();
        }
        var reviewPreconditionsMet = true;
        if (task instanceof ProtectedVerificationRequestTask pvTask) {
            reviewPreconditionsMet = pvTask.hasOpenedZasData();
        }
        return resolvePossibleActions(
            task.getStatus(),
            task.getAssignee(),
            currentUserFullName,
            canRequestMoreInformation,
            reviewPreconditionsMet
        );
    }

    /**
     * @param reviewPreconditionsMet whether any task-type-specific precondition for APPROVE/REJECT has been met
     *     (e.g. for a protected verification request, that ZAS data has been reviewed). Pass {@code true} for task
     *     types without such a precondition.
     */
    private static Set<TaskActionDto> resolvePossibleActions(
        TaskStatus currentStatus,
        String currentAssignee,
        String currentUserFullName,
        boolean canRequestMoreInformation,
        boolean reviewPreconditionsMet
    ) {
        var actions = new HashSet<TaskActionDto>();
        for (var action : TaskActionDto.values()) {
            switch (action) {
                case REJECT -> {
                    if (isValidNewStatus(currentStatus, TaskStatus.REJECTED) && reviewPreconditionsMet) {
                        actions.add(action);
                    }
                }
                case APPROVE -> {
                    if (isValidNewStatus(currentStatus, TaskStatus.ACCEPTED) && reviewPreconditionsMet) {
                        actions.add(action);
                    }
                }
                case REQUEST_MORE_INFORMATION -> {
                    if (
                        isValidNewStatus(currentStatus, TaskStatus.INFORMATION_REQUESTED) && canRequestMoreInformation
                    ) {
                        actions.add(action);
                    }
                }
                case ADD_INTERNAL_NOTE -> actions.add(action); // always possible for now
                case ASSIGN_SELF -> {
                    // only if not already assigned to current user
                    if (!Objects.equals(currentAssignee, currentUserFullName)) {
                        actions.add(action);
                    }
                }
            }
        }
        return actions;
    }
}
