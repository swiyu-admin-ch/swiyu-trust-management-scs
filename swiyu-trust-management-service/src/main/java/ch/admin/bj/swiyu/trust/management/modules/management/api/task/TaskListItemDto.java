package ch.admin.bj.swiyu.trust.management.modules.management.api.task;

import ch.admin.bj.swiyu.trust.management.modules.common.i18n.ValidLocalizedMap;
import ch.admin.bj.swiyu.trust.management.modules.management.api.task.taskaction.TaskActionDto;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.time.Instant;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

/**
 *
 * @param submissionId The originating submission id the task was resolved from. E.g. the id of the
 *                     TrustOnboardingSubmission if it is a TrustOnboardingTask. Null for deletion requests, which originate in the TMS.
 */
@Schema(name = "TaskListItem", enumAsRef = true)
public record TaskListItemDto(
    @NotNull UUID id,
    @NotNull @ValidLocalizedMap Map<String, @NotBlank String> partnerName,
    @NotNull Instant submittedAt,
    @NotNull Instant dueAt,
    @NotNull TaskStatusDto state,
    String assignee,
    @NotNull String taskType,
    @NotNull Set<TaskActionDto> allowedActions,
    UUID submissionId
) {}
