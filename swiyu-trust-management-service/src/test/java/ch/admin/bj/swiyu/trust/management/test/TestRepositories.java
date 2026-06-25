package ch.admin.bj.swiyu.trust.management.test;

import ch.admin.bj.swiyu.trust.management.modules.management.domain.NonCompliantActorRepository;
import ch.admin.bj.swiyu.trust.management.modules.management.domain.TrustStatementPartnerLinkRepository;
import ch.admin.bj.swiyu.trust.management.modules.management.domain.domainevent.DomainEventLogRepository;
import ch.admin.bj.swiyu.trust.management.modules.registry.domain.NonComplianceListRepository;
import lombok.AllArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.test.context.transaction.TestTransaction;

/**
 * Convenience class for Integration Tests for the following reasons:
 *
 * <ul>
 *     <li>Simple access for cleaning data in between tests and commiting transactions</li>
 *     <li>Simple access to a repo (inject only this one)</li>
 * </ul>
 */
@Component
@AllArgsConstructor
public class TestRepositories {

    public final NonCompliantActorRepository nonCompliantActor;
    public final DomainEventLogRepository domainEventLog;
    public final NonComplianceListRepository nonComplianceList;
    public final TrustStatementPartnerLinkRepository trustStatementPartnerLink;

    /**
     * Opens a new Transaction. Might be useful if there are multiple transactions are needed within a test.
     */
    public void startNewTransaction() {
        TestTransaction.start();
    }

    public void commit() {
        TestTransaction.flagForCommit();
        TestTransaction.end();
    }
}
