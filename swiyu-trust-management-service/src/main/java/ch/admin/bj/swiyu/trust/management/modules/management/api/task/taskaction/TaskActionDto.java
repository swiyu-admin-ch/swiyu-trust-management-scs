package ch.admin.bj.swiyu.trust.management.modules.management.api.task.taskaction;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(name = "TaskAction", enumAsRef = true)
public enum TaskActionDto {
    REJECT,
    APPROVE,
    REQUEST_MORE_INFORMATION,
    ADD_INTERNAL_NOTE,
    ASSIGN_SELF,
}
