package ch.admin.bj.swiyu.trust.management.modules.jobs.service;

import ch.admin.bj.swiyu.trust.management.modules.common.async.Lock;
import ch.admin.bj.swiyu.trust.management.modules.common.security.SecurityContextSupport;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.javacrumbs.shedlock.spring.annotation.SchedulerLock;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

/**
 * Identifies trust statements which exist in the registry but not in the management database and repairs them.
 * <p>
 * The job runs once per instance, delayed so it does not slow down the startup, and does not repeat for the
 * lifetime of the instance ({@code app.jobs.lost-statements-identification.repeat-delay}). ShedLock keeps the
 * cluster to one run at a time. As the work is idempotent, a second instance running it again is wasteful, but
 * never changes the outcome.
 */
@Slf4j
@Service
@RequiredArgsConstructor
@ConditionalOnProperty(
    prefix = "app.jobs.lost-statements-identification",
    name = "enabled",
    havingValue = "true",
    matchIfMissing = true
)
public class LostStatementsIdentificationJob {

    private final LostStatementsIdentificationService lostStatementsIdentificationService;

    @Scheduled(
        initialDelayString = "${app.jobs.lost-statements-identification.initial-delay}",
        fixedDelayString = "${app.jobs.lost-statements-identification.repeat-delay}"
    )
    @SchedulerLock(name = Lock.LOST_STATEMENTS_IDENTIFICATION, lockAtMostFor = "PT2H", lockAtLeastFor = "PT10S")
    public void identifyLostStatements() {
        var previousAuthentication = SecurityContextHolder.getContext().getAuthentication();
        SecurityContextSupport.setSystemUserAuthentication();
        try {
            log.info(
                "LostStatementsIdentification: started, identifying statements which exist in the registry, but not in the management DB"
            );

            var modifiedStatementCount = lostStatementsIdentificationService.resolveV2Statements();
            modifiedStatementCount += lostStatementsIdentificationService.resolveV1Statements();

            if (modifiedStatementCount > 0) {
                log.warn("LostStatementsIdentification: finished, edited {} statements", modifiedStatementCount);
            } else {
                log.info("LostStatementsIdentification: finished, edited {} statements", modifiedStatementCount);
            }
        } catch (Exception e) {
            log.error("LostStatementsIdentification: failed!", e);
        } finally {
            SecurityContextHolder.getContext().setAuthentication(previousAuthentication);
        }
    }
}
