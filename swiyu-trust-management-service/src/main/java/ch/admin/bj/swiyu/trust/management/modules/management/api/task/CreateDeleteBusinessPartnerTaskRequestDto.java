package ch.admin.bj.swiyu.trust.management.modules.management.api.task;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.util.UUID;

@Schema(name = "CreateDeleteBusinessPartnerTaskRequest")
public record CreateDeleteBusinessPartnerTaskRequestDto(
    @NotNull UUID businessPartnerId,
    @Schema(description = "Why the partner is to be deleted. Kept as the internal note of the task.")
    @NotBlank
    String reason,
    @Schema(
        description = "Must be true: confirms the reviewer understood that the partner is deleted irreversibly once the task becomes due."
    )
    @NotNull
    @AssertTrue
    Boolean irreversibleDeletionConfirmed
) {}
