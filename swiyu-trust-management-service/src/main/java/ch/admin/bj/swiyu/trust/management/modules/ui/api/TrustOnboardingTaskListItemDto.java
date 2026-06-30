package ch.admin.bj.swiyu.trust.management.modules.ui.api;

import ch.admin.bj.swiyu.trust.management.modules.common.i18n.ValidLocalizedMap;
import ch.admin.bj.swiyu.trust.management.modules.management.api.TrustOnboardingTaskActionDto;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.time.Instant;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

@Schema(name = "TrustOnboardingTaskListItem", enumAsRef = true)
public record TrustOnboardingTaskListItemDto(
    @NotNull UUID id,
    @NotNull @ValidLocalizedMap Map<String, @NotBlank String> partnerName,
    @NotNull Instant submittedAt,
    @NotNull Instant dueAt,
    @NotNull TrustOnboardingTaskStatusDto state,
    String assignee,
    @NotNull String taskType,
    @NotNull Set<TrustOnboardingTaskActionDto> allowedActions
) {}
