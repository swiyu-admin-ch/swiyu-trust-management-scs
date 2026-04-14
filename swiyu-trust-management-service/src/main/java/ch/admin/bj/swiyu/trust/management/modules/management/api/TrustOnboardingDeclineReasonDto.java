package ch.admin.bj.swiyu.trust.management.modules.management.api;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(name = "TrustOnboardingDeclineReason", enumAsRef = true)
public enum TrustOnboardingDeclineReasonDto {
    MISSING_DOCUMENTS,
    UNAUTHORIZED_SIGNATORIES,
    INCORRECT_COMPANY_INFORMATION,
    INCORRECT_DECLARATION_OF_INTENT,
    OTHER,
}
