package ch.admin.bj.swiyu.trust.management.modules.ui.api.taskaction;

import ch.admin.bj.swiyu.trust.management.modules.management.api.TrustOnboardingRejectReasonDto;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;

@Schema(name = "RejectTaskAction")
public record RejectTaskActionDto(
    @NotNull TrustOnboardingRejectReasonDto rejectReason,
    String partnerNote,
    String internalNote
) {}
