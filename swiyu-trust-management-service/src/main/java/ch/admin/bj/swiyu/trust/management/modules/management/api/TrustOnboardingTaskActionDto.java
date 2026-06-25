package ch.admin.bj.swiyu.trust.management.modules.management.api;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(name = "TrustOnboardingTaskAction", enumAsRef = true)
public enum TrustOnboardingTaskActionDto {
    REJECT,
    APPROVE,
    REQUEST_MORE_INFORMATION,
    ADD_INTERNAL_NOTE,
    ASSIGN_SELF,
}
