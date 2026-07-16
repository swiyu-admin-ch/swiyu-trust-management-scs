package ch.admin.bj.swiyu.trust.management.modules.management.api.taskaction;

import ch.admin.bj.swiyu.trust.management.modules.management.api.TrustOnboardingDeclineReasonDto;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;

@Schema(name = "RequestMoreInformationTaskAction")
public record RequestMoreInformationTaskActionDto(
    @NotNull TrustOnboardingDeclineReasonDto declineReason,
    @NotNull String partnerNote,
    String internalNote
) {}
