package ch.admin.bj.swiyu.trust.management.modules.management.api.taskaction;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;

@Schema(name = "RejectProtectedVerificationRequestTaskAction")
public record RejectProtectedVerificationRequestTaskActionDto(@NotBlank String rejectReason, String internalNote) {}
