package ch.admin.bj.swiyu.trust.management.modules.management.api.task;

import io.swagger.v3.oas.annotations.media.Schema;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.RequestParam;

@Schema(name = "TaskFilter")
public record TaskFilterDto(
    @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate submissionStartDate,
    @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate submissionEndDate,
    @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate dueStartDate,
    @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate dueEndDate,
    @RequestParam(required = false) List<String> state,
    @RequestParam(required = false) String assignee,
    @RequestParam(required = false) List<String> taskType,
    // An instant rather than a date on purpose: a date would also select tasks that only become due later that day,
    // and approving them would cut the waiting period short.
    @Schema(
        description = "Only delete-business-partner tasks whose hardDeleteDueAt is at or before this instant, i.e. that are due for auto-approval."
    )
    @RequestParam(required = false)
    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
    Instant hardDeleteDueUntil
) {}
