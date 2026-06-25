package ch.admin.bj.swiyu.trust.management.modules.management.service;

import ch.admin.bj.swiyu.trust.management.modules.common.async.Lock;
import ch.admin.bj.swiyu.trust.management.modules.common.security.SystemUserAuthentication;
import ch.admin.bj.swiyu.trust.management.modules.management.domain.StatusListDomainService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.javacrumbs.shedlock.spring.annotation.SchedulerLock;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.security.core.context.SecurityContextHolder;
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
public class StatementRefreshScheduler {

    private final StatusListDomainService statusListDomainService;
    private final NonCompliantActorPublicationService nonCompliantActorPublicationService;
    private final TrustStatementService trustStatementService;

    @Scheduled(cron = "${app.statement-defaults.non-compliance-trust-list-statement.refresh-cron}")
    @SchedulerLock(name = Lock.NON_COMPLIANCE_TRUST_LIST_PUBLISHING)
    public void refreshNonComplianceList() {
        SecurityContextHolder.getContext().setAuthentication(new SystemUserAuthentication());
        nonCompliantActorPublicationService.triggerPublication();
    }

    @Scheduled(cron = "${app.statement-defaults.protected-issuance-trust-list-statement.refresh-cron}")
    @SchedulerLock(name = Lock.PROTECTED_ISSUANCE_TRUST_LIST_PUBLISHING)
    public void refreshProtectedIssuanceTrustList() {
        SecurityContextHolder.getContext().setAuthentication(new SystemUserAuthentication());
        trustStatementService.issueAndPublishProtectedIssuanceV2TrustListStatement();
    }

    @Scheduled(cron = "${app.statement-defaults.statuslist.refresh-cron}")
    @SchedulerLock(name = Lock.STATUS_LIST_PUBLISHING)
    public void refreshStatusLists() {
        SecurityContextHolder.getContext().setAuthentication(new SystemUserAuthentication());
        statusListDomainService.triggerPublications();
    }
}
