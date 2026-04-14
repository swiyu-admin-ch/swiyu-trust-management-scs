package ch.admin.bj.swiyu.trust.management.modules.management.domain;

import static org.junit.jupiter.api.Assertions.assertThrows;

import ch.admin.bj.swiyu.trust.management.modules.common.exception.TrustOnboardingTaskStatusValidationException;
import org.junit.jupiter.api.Test;

class TrustTaskStatusValidatorTest {

    @Test
    public void testValidateNewStatus() {
        assertThrows(TrustOnboardingTaskStatusValidationException.class, () ->
            TrustTaskStatusValidator.validateNewStatus(TrustTaskStatus.ACCEPTED, TrustTaskStatus.OPENED)
        );
        assertThrows(TrustOnboardingTaskStatusValidationException.class, () ->
            TrustTaskStatusValidator.validateNewStatus(TrustTaskStatus.REJECTED, TrustTaskStatus.OPENED)
        );
        TrustTaskStatusValidator.validateNewStatus(TrustTaskStatus.OPENED, TrustTaskStatus.INFORMATION_REQUESTED);
        TrustTaskStatusValidator.validateNewStatus(TrustTaskStatus.OPENED, TrustTaskStatus.REJECTED);
        TrustTaskStatusValidator.validateNewStatus(TrustTaskStatus.INFORMATION_REQUESTED, TrustTaskStatus.OPENED);
        TrustTaskStatusValidator.validateNewStatus(TrustTaskStatus.INFORMATION_REQUESTED, TrustTaskStatus.REJECTED);
    }
}
