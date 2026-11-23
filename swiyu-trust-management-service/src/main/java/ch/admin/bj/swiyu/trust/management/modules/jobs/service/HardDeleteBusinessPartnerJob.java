package ch.admin.bj.swiyu.trust.management.modules.jobs.service;

import static ch.admin.bj.swiyu.trust.management.modules.common.security.SecurityContextSupport.SYSTEM_USER;
import static ch.admin.bj.swiyu.trust.management.modules.common.security.SecurityContextSupport.setSystemUserAuthentication;

import ch.admin.bj.swiyu.trust.management.modules.common.async.Lock;
import ch.admin.bj.swiyu.trust.management.modules.management.api.task.TaskFilterDto;
import ch.admin.bj.swiyu.trust.management.modules.management.api.task.TaskStatusDto;
import ch.admin.bj.swiyu.trust.management.modules.management.service.DeleteBusinessPartnerTaskService;
import ch.admin.bj.swiyu.trust.management.modules.management.service.TaskService;
import java.time.Instant;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.javacrumbs.shedlock.spring.annotation.SchedulerLock;
import org.springframework.data.domain.Pageable;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

/**
 * Approves the delete-business-partner tasks whose waiting period is over, which orders the deletion of the partner in
 * the core business service.
 *
 * <p>Approval is per task and in its own transaction: one partner failing (for example because the core business service
 * withdrew its hard-delete permission) must not hold back the deletion of another. A failed or skipped task stays
 * {@code OPENED} and is picked up again on the next run.
 *
 * <p>The job is not gated by the {@code EIDARTFE_1571} feature flag. Only {@code createTask} creates a delete task and
 * that path is gated, so every task this job sees was requested while the feature was on. A pending task can outlive
 * the flag only when it is switched off after the request - and since the flag is bound at startup, that is a rollout,
 * in which this job is stopped by setting {@code app.jobs.hard-delete-business-partner-cron} to {@code -} (or by
 * rejecting the task, which is not gated). Calling a pending deletion off is audited through that rejection and through
 * the hard-delete permission in the core business service.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class HardDeleteBusinessPartnerJob {

    /**
     * The filter takes the task type as the string the API uses, so the job does not reach into the task domain (the
     * jobs module only depends on the management API and services).
     */
    private static final String DELETE_BUSINESS_PARTNER_TASK_TYPE = "DELETE_BUSINESS_PARTNER";

    private final TaskService taskService;
    private final DeleteBusinessPartnerTaskService deleteBusinessPartnerTaskService;

    @Scheduled(cron = "${app.jobs.hard-delete-business-partner-cron}")
    @SchedulerLock(name = Lock.HARD_DELETE_BUSINESS_PARTNER)
    public void approveDeleteTasksThatAreDue() {
        setSystemUserAuthentication();

        var dueTasks = taskService.getTasks(Pageable.unpaged(), dueForApprovalFilter(), SYSTEM_USER).getContent();
        var failed = 0;
        for (var task : dueTasks) {
            try {
                // SYSTEM as the triggering user: the domain event says the job approved the task. Who requested the
                // deletion is in the task itself and ends up in the command.
                deleteBusinessPartnerTaskService.approve(task.id(), SYSTEM_USER);
            } catch (RuntimeException e) {
                failed++;
                log.error("Failed to approve deletion task {}, retrying on the next run", task.id(), e);
            }
        }
        if (!dueTasks.isEmpty()) {
            log.info(
                "Approved {} of {} due deletion task(s), {} left open",
                dueTasks.size() - failed,
                dueTasks.size(),
                failed
            );
        }
    }

    /**
     * {@code hardDeleteDueUntil} is an instant, not a date: the upper bound must not include tasks whose waiting period
     * ends later today. {@code OPENED} is the only pending state a delete task can be in - it has no request for more
     * information.
     */
    private static TaskFilterDto dueForApprovalFilter() {
        return new TaskFilterDto(
            null,
            null,
            null,
            null,
            List.of(TaskStatusDto.OPENED.name()),
            null,
            List.of(DELETE_BUSINESS_PARTNER_TASK_TYPE),
            Instant.now()
        );
    }
}
