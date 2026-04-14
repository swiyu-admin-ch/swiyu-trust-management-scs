package ch.admin.bj.swiyu.trust.management.modules.ui.api.taskaction;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;

@Schema(name = "AddInternalNoteTaskAction")
public record AddInternalNoteTaskActionDto(@NotNull String internalNote) {}
