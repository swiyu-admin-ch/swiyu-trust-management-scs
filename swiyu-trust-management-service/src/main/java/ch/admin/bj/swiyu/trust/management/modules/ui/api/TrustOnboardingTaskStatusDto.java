package ch.admin.bj.swiyu.trust.management.modules.ui.api;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(name = "TrustOnboardingTaskStatus", enumAsRef = true)
public enum TrustOnboardingTaskStatusDto {
    REJECTED,
    ACCEPTED,
    OPENED,
    INFORMATION_REQUESTED,
}
