package ch.admin.bj.swiyu.trust.management.modules.jobs.service;

import ch.admin.bj.swiyu.trust.management.modules.common.async.Lock;
import ch.admin.bj.swiyu.trust.management.modules.dataimport.service.DemoDataImportService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.javacrumbs.shedlock.spring.annotation.SchedulerLock;
import org.springframework.context.annotation.Profile;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

/**
 * Seeds the local database with demo data. Only active with the {@code test-data-injection} profile.
 * <p>
 * Like the other startup jobs it runs once per instance, delayed after startup, and does not repeat for the
 * lifetime of the instance. Being a {@code @Scheduled} method, Spring Boot observes it, so its log lines carry
 * the run's trace id.
 */
@Slf4j
@Service
@RequiredArgsConstructor
@Profile("test-data-injection")
public class DemoDataImportJob {

    private final DemoDataImportService demoDataImportService;

    @Scheduled(
        initialDelayString = "${app.jobs.demo-data-import.initial-delay}",
        fixedDelayString = "${app.jobs.demo-data-import.repeat-delay}"
    )
    @SchedulerLock(name = Lock.DEMO_DATA_IMPORT, lockAtMostFor = "PT2H", lockAtLeastFor = "PT10S")
    public void importDemoData() {
        var previousAuthentication = SecurityContextHolder.getContext().getAuthentication();
        // setting system auth for AuditMetadata
        demoDataImportService.setSystemSecurityContext();
        try {
            log.warn("LOCAL TEST DATA INJECTION is happening!");

            demoDataImportService.deleteTrustOnboardingTasks();
            demoDataImportService.deleteProtectedVerificationRequestTasks();
            demoDataImportService.deleteProtectedVerificationAuthorizations();
            demoDataImportService.deleteProtectedIssuanceEntriesAndAuthorizations();
            demoDataImportService.deleteTrustStatementPartnerLinks();
            demoDataImportService.deleteBusinessPartnerIdentities();
            demoDataImportService.loadBusinessPartnerIdentities();
            demoDataImportService.loadProtectedVerificationAuthorizations();
            demoDataImportService.loadTrustOnboardingTasks();
            demoDataImportService.loadProtectedVerificationRequestTasks();
            demoDataImportService.loadProtectedIssuanceDemoData();

            log.debug("LOCAL TEST DATA INJECTION is done!");
        } catch (Exception e) {
            log.error("LOCAL TEST DATA INJECTION failed!", e);
        } finally {
            SecurityContextHolder.getContext().setAuthentication(previousAuthentication);
        }
    }
}
