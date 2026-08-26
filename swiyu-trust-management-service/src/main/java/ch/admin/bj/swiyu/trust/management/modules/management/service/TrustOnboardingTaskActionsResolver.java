package ch.admin.bj.swiyu.trust.management.modules.management.service;

import static ch.admin.bj.swiyu.trust.management.modules.management.domain.TrustTaskStatusValidator.isValidNewStatus;

import ch.admin.bj.swiyu.trust.management.modules.management.api.TrustOnboardingTaskActionDto;
import ch.admin.bj.swiyu.trust.management.modules.management.domain.TrustTaskStatus;
import java.util.HashSet;
import java.util.Objects;
import java.util.Set;
import lombok.experimental.UtilityClass;

@UtilityClass
public class TrustOnboardingTaskActionsResolver {

    public static Set<TrustOnboardingTaskActionDto> resolvePossibleActions(
        TrustTaskStatus currentStatus,
        String currentAssignee,
        String currentUserFullName
    ) {
        return resolvePossibleActions(currentStatus, currentAssignee, currentUserFullName, true, true);
    }

    public static Set<TrustOnboardingTaskActionDto> resolvePossibleActions(
        TrustTaskStatus currentStatus,
        String currentAssignee,
        String currentUserFullName,
        boolean canRequestMoreInformation
    ) {
        return resolvePossibleActions(
            currentStatus,
            currentAssignee,
            currentUserFullName,
            canRequestMoreInformation,
            true
        );
    }

    /**
     * @param reviewPreconditionsMet whether any task-type-specific precondition for APPROVE/REJECT has been met
     *     (e.g. for a protected verification request, that ZAS data has been reviewed). Pass {@code true} for task
     *     types without such a precondition.
     */
    public static Set<TrustOnboardingTaskActionDto> resolvePossibleActions(
        TrustTaskStatus currentStatus,
        String currentAssignee,
        String currentUserFullName,
        boolean canRequestMoreInformation,
        boolean reviewPreconditionsMet
    ) {
        var actions = new HashSet<TrustOnboardingTaskActionDto>();
        for (var action : TrustOnboardingTaskActionDto.values()) {
            switch (action) {
                case REJECT -> {
                    if (isValidNewStatus(currentStatus, TrustTaskStatus.REJECTED) && reviewPreconditionsMet) {
                        actions.add(action);
                    }
                }
                case APPROVE -> {
                    if (isValidNewStatus(currentStatus, TrustTaskStatus.ACCEPTED) && reviewPreconditionsMet) {
                        actions.add(action);
                    }
                }
                case REQUEST_MORE_INFORMATION -> {
                    if (
                        isValidNewStatus(currentStatus, TrustTaskStatus.INFORMATION_REQUESTED) &&
                        canRequestMoreInformation
                    ) {
                        actions.add(action);
                    }
                }
                case ADD_INTERNAL_NOTE -> {
                    actions.add(action); // always possible for now
                }
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
