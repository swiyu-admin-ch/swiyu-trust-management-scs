package ch.admin.bj.swiyu.trust.management.modules.ui.api;

import ch.admin.bj.swiyu.trust.management.modules.common.i18n.ValidLocalizedMap;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.time.Instant;
import java.util.Map;
import java.util.UUID;

@Schema(name = "TrustAddDidTask")
public record TrustAddDidTaskDto(
    @NotNull UUID id,
    String assignee,
    @NotNull Instant submittedAt,
    @NotNull Instant dueAt,
    @NotNull TrustOnboardingTaskStatusDto state,
    @NotNull @ValidLocalizedMap Map<String, @NotBlank String> partnerName,
    @NotNull String permissionDid,
    @NotNull UUID trustAddDidSubmissionId
) {}
