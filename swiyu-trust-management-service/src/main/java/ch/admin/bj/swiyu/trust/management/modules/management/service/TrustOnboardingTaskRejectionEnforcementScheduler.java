package ch.admin.bj.swiyu.trust.management.modules.management.service;

import ch.admin.bj.swiyu.trust.management.modules.common.async.Lock;
import ch.admin.bj.swiyu.trust.management.modules.common.security.SystemUserAuthentication;
import lombok.RequiredArgsConstructor;
import net.javacrumbs.shedlock.spring.annotation.SchedulerLock;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class TrustOnboardingTaskRejectionEnforcementScheduler {

    private final TrustOnboardingTaskService trustOnboardingTaskService;

    @Scheduled(cron = "${app.trust-onboarding-task.rejection-enforcement-check-cron}")
    @SchedulerLock(name = Lock.TRUST_ONBOARDING_TASK_REJECTION_ENFORCEMENT)
    public void rejectTasksPastRejectionEnforcementDeadline() {
        SecurityContextHolder.getContext().setAuthentication(new SystemUserAuthentication());
        trustOnboardingTaskService.rejectTasksPastRejectionEnforcementDeadline();
    }
}
