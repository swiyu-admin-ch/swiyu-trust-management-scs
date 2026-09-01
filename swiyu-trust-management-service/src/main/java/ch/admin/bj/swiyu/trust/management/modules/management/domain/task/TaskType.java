package ch.admin.bj.swiyu.trust.management.modules.management.domain.task;

/**
 * Type of a trust task. For trust onboarding tasks this is the submission subtype mirrored from the
 * core-business-service (REGISTRATION for a first-time onboarding, PROFILE_CHANGE for an already
 * onboarded partner; RENEWAL is not emitted yet). ADD_DID is the add-DID task type.
 */
public enum TaskType {
    REGISTRATION,
    PROFILE_CHANGE,
    RENEWAL,
    ADD_DID,
    PROTECTED_VERIFICATION_REQUEST,
}
