package ch.admin.bj.swiyu.trust.management.modules.ui.api.taskaction;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(name = "ApproveTaskAction")
public record ApproveTaskActionDto(String partnerNote, String internalNote) {}
