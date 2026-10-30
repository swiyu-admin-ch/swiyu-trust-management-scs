package ch.admin.bj.swiyu.trust.management.modules.common.exception;

import lombok.Getter;

/**
 * Raised when a delete-business-partner task is requested for a partner the core business service has not cleared for
 * deletion, or that already has a pending deletion request. Surfaces as a 400 carrying the {@link #error} code, which
 * the UI translates; the message stays server-side for the log.
 */
@Getter
public class BusinessPartnerNotDeletableException extends RuntimeException {

    private final BusinessPartnerDeletionError error;

    public BusinessPartnerNotDeletableException(BusinessPartnerDeletionError error, String message) {
        super(message);
        this.error = error;
    }
}
