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
        var actions = new HashSet<TrustOnboardingTaskActionDto>();
        for (var action : TrustOnboardingTaskActionDto.values()) {
            switch (action) {
                case REJECT -> {
                    if (isValidNewStatus(currentStatus, TrustTaskStatus.REJECTED)) {
                        actions.add(action);
                    }
                }
                case APPROVE -> {
                    if (isValidNewStatus(currentStatus, TrustTaskStatus.ACCEPTED)) {
                        actions.add(action);
                    }
                }
                case REQUEST_MORE_INFORMATION -> {
                    if (isValidNewStatus(currentStatus, TrustTaskStatus.INFORMATION_REQUESTED)) {
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
