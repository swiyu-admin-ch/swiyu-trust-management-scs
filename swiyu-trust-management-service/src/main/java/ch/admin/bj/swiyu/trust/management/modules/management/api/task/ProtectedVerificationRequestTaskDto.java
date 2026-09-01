package ch.admin.bj.swiyu.trust.management.modules.management.api.task;

import ch.admin.bj.swiyu.trust.management.modules.common.i18n.ValidLocalizedMap;
import ch.admin.bj.swiyu.trust.management.modules.management.api.AuthorizableFieldDto;
import ch.admin.bj.swiyu.trust.management.modules.management.api.task.taskaction.TaskActionDto;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.time.Instant;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

@Schema(name = "ProtectedVerificationRequestTask")
public record ProtectedVerificationRequestTaskDto(
    @NotNull UUID id,
    String assignee,
    @NotNull Instant submittedAt,
    Instant dueAt,
    @NotNull TaskStatusDto state,
    @NotNull @ValidLocalizedMap Map<String, @NotBlank String> partnerName,
    UUID businessPartnerId,
    @NotNull UUID protectedVerificationSubmissionId,
    String uid,
    ContactPersonDto contactPerson,
    String reason,
    UUID sbnId,
    @NotNull AuthorizableFieldDto category,
    boolean zasDataOpened,
    @NotNull Set<TaskActionDto> allowedActions
) {
    @Schema(name = "ProtectedVerificationRequestTaskContactPerson")
    public record ContactPersonDto(String firstName, String lastName, String email, String phone) {}
}
