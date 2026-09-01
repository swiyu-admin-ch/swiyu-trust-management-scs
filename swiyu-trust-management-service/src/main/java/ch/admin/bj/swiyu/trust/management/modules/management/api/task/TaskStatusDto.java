package ch.admin.bj.swiyu.trust.management.modules.management.api.task;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(name = "TaskStatus", enumAsRef = true)
public enum TaskStatusDto {
    REJECTED,
    ACCEPTED,
    OPENED,
    INFORMATION_REQUESTED,
    RESUBMITTED,
}
