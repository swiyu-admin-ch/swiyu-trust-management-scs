package ch.admin.bj.swiyu.trust.management.modules.management.api.task;

import io.swagger.v3.oas.annotations.media.Schema;

/**
 * Which of the trust onboarding flows a task belongs to. Only these three can reach a trust onboarding task - the
 * domain enforces that in {@code TrustOnboardingTask.validateTaskType}, and this enum says the same in the type, so
 * the UI can switch over it instead of comparing strings. The mapping from the domain type lives in {@code TaskMapper}
 * because this layer must not depend on the domain.
 */
@Schema(name = "TrustOnboardingTaskType")
public enum TrustOnboardingTaskTypeDto {
    /** A first-time onboarding. */
    REGISTRATION,
    /** An already onboarded partner changes relevant information. */
    PROFILE_CHANGE,
    /** An extension of the validity without a relevant change, approved automatically. */
    RENEWAL,
}
