package ch.admin.bj.swiyu.trust.management.modules.common.exception;

import lombok.AllArgsConstructor;
import lombok.Getter;

@AllArgsConstructor
@Getter
public class TrustOnboardingTaskStatusValidationException extends RuntimeException {

    public TrustOnboardingTaskStatusValidationException(String message) {
        super(message);
    }

    public TrustOnboardingTaskStatusValidationException(String message, Throwable cause) {
        super(message, cause);
    }
}
