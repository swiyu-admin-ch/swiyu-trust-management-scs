package ch.admin.bj.swiyu.trust.management.modules.ui.api;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import java.time.Instant;
import java.util.UUID;

@Schema(name = "TrustAddDidTask")
public record TrustAddDidTaskDto(
    @NotNull UUID id,
    String assignee,
    @NotNull Instant submittedAt,
    @NotNull Instant dueAt,
    @NotNull TrustOnboardingTaskStatusDto state,
    @NotNull MultiLanguageTextDto partnerName,
    @NotNull String permissionDid,
    @NotNull UUID trustAddDidSubmissionId
) {}
