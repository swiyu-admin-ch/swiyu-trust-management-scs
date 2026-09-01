package ch.admin.bj.swiyu.trust.management.modules.management.api.task.taskaction;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;

@Schema(name = "RequestMoreInformationTaskAction")
public record RequestMoreInformationTaskActionDto(@NotNull String partnerNote, String internalNote) {}
