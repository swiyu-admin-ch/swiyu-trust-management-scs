package ch.admin.bj.swiyu.trust.management.modules.management.api;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(name = "TrustOnboardingRejectReason", enumAsRef = true)
public enum TrustOnboardingRejectReasonDto {
    INCOMPLETE_INFORMATION,
    INACCURATE_INFORMATION,
    OUTDATED_INFORMATION,
    IDENTITY_VERIFICATION_FAILURE,
    LACK_OF_AUTHORIZATION,
    TECHNICAL_ISSUES,
    DUPLICATE_APPLICATION,
    NO_RESPONSE_FROM_APPLICANT,
    FRAUDULENT_ACTIVITY,
    OTHER,
}
