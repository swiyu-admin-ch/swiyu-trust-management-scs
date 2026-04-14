package ch.admin.bj.swiyu.trust.management.modules.management.domain;

import ch.admin.bj.swiyu.trust.management.modules.common.exception.TrustOnboardingTaskStatusValidationException;
import java.util.Set;

public class TrustTaskStatusValidator {

    public static void validateNewStatus(TrustTaskStatus currentStatus, TrustTaskStatus newStatus) {
        if (!isValidNewStatus(currentStatus, newStatus)) {
            throw new TrustOnboardingTaskStatusValidationException(
                "Cannot change status from " + currentStatus + " to " + newStatus
            );
        }
    }

    public static boolean isValidNewStatus(TrustTaskStatus currentStatus, TrustTaskStatus newStatus) {
        return getPossibleNewStatus(currentStatus).contains(newStatus);
    }

    private static Set<TrustTaskStatus> getPossibleNewStatus(TrustTaskStatus currentStatus) {
        return switch (currentStatus) {
            case OPENED -> Set.of(
                TrustTaskStatus.INFORMATION_REQUESTED,
                TrustTaskStatus.ACCEPTED,
                TrustTaskStatus.REJECTED
            );
            case INFORMATION_REQUESTED -> Set.of(
                TrustTaskStatus.OPENED,
                TrustTaskStatus.ACCEPTED,
                TrustTaskStatus.REJECTED
            );
            case ACCEPTED, REJECTED -> Set.of();
        };
    }
}
