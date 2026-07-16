package ch.admin.bj.swiyu.trust.management.modules.jobs.service;

import static ch.admin.bj.swiyu.trust.management.modules.common.security.SecurityContextSupport.setSystemUserAuthentication;

import ch.admin.bj.swiyu.trust.management.modules.common.async.Lock;
import ch.admin.bj.swiyu.trust.management.modules.management.service.NonCompliantActorPublicationService;
import ch.admin.bj.swiyu.trust.management.modules.management.service.StatusListService;
import ch.admin.bj.swiyu.trust.management.modules.management.service.TrustStatementService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.javacrumbs.shedlock.spring.annotation.SchedulerLock;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

/**
 * Re-issues statuslists and statements which have a valid business lifetime, but a smaller technical lifetime.
 * <p>
 * Example: Statuslist have an unlimited business lifetime,
 * but should technically be only living 1 week max in any (external) cache.
 * <p>
 * For each cron we have a max lifetime of time-to-live for the given token (statuslist/statement).
 * If we do have an error in the refresh of this token this is the MAX time we have to solve the problem before the ecosystem is in disarray.
 * The refresh-cron MUST therefore be a fraction of the time-to-live.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class StatementRefreshJob {

    private final StatusListService statusListService;
    private final NonCompliantActorPublicationService nonCompliantActorPublicationService;
    private final TrustStatementService trustStatementService;

    @Scheduled(cron = "${app.jobs.statement-non-compliance-trust-list-refresh-interval}")
    @SchedulerLock(name = Lock.NON_COMPLIANCE_TRUST_LIST_PUBLISHING)
    public void refreshNonComplianceList() {
        setSystemUserAuthentication();
        nonCompliantActorPublicationService.triggerPublication();
    }

    @Scheduled(cron = "${app.jobs.statement-protected-issuance-trust-list-refresh-interval}")
    @SchedulerLock(name = Lock.PROTECTED_ISSUANCE_TRUST_LIST_PUBLISHING)
    public void refreshProtectedIssuanceTrustList() {
        setSystemUserAuthentication();
        trustStatementService.issueAndPublishProtectedIssuanceV2TrustListStatement();
    }

    @Scheduled(cron = "${app.jobs.statement-status-list-refresh-interval}")
    @SchedulerLock(name = Lock.STATUS_LIST_PUBLISHING)
    public void refreshStatusLists() {
        setSystemUserAuthentication();
        statusListService.triggerPublications();
    }
}
