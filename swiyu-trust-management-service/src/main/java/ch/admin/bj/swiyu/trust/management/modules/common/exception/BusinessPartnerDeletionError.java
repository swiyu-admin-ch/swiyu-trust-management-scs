package ch.admin.bj.swiyu.trust.management.modules.common.exception;

/**
 * Why a business partner cannot be deleted. This code, not the exception message, is what the UI-API returns: the
 * message is English only and contains technical ids, while the UI translates the code into the user's language.
 */
public enum BusinessPartnerDeletionError {
    /** The core business service has not synced the partner yet, so there is nothing cleared for deletion. */
    PARTNER_NOT_SYNCED,
    /** The core business service owns the decision and has not set the hard-delete flag. */
    PARTNER_NOT_CLEARED_FOR_DELETION,
    /** A deletion request for this partner is already pending. */
    OPEN_DELETION_REQUEST_EXISTS,
}
