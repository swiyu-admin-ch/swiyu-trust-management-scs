package ch.admin.bj.swiyu.trust.management.modules.management.api.task.taskaction;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;

/**
 * Rejecting a deletion request carries no {@code rejectReason} enum: unlike an onboarding rejection, nothing is sent
 * back to the partner, so only the internal note is kept.
 */
@Schema(name = "RejectDeleteBusinessPartnerTaskAction")
public record RejectDeleteBusinessPartnerTaskActionDto(
    @Schema(description = "Why the deletion was called off. Kept as the internal note of the task.")
    @NotBlank
    String internalNote
) {}
