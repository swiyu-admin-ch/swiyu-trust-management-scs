package ch.admin.bj.swiyu.trust.management.modules.management.api.task;

import ch.admin.bj.swiyu.trust.management.modules.management.api.task.taskaction.TaskActionDto;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.time.Instant;
import java.util.Set;
import java.util.UUID;

/**
 * @param partnerName The name of the partner as the core business service projection has it - a single string, not a
 *     localized map, because a partner name has no translations.
 */
@Schema(name = "DeleteBusinessPartnerTask")
public record DeleteBusinessPartnerTaskDto(
    @NotNull UUID id,
    String assignee,
    @NotNull Instant submittedAt,
    Instant dueAt,
    @NotNull TaskStatusDto state,
    @NotBlank String partnerName,
    @NotNull UUID businessPartnerId,
    @Schema(description = "When the partner becomes due for irreversible deletion.") @NotNull Instant hardDeleteDueAt,
    @NotNull String taskType,
    @NotNull Set<TaskActionDto> allowedActions
) {}
