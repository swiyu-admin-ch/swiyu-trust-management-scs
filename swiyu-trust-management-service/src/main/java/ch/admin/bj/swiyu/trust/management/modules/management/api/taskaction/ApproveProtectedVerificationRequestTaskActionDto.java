package ch.admin.bj.swiyu.trust.management.modules.management.api.taskaction;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(name = "ApproveProtectedVerificationRequestTaskAction")
public record ApproveProtectedVerificationRequestTaskActionDto(String internalNote) {}
