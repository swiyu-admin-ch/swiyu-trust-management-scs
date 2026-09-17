package ch.admin.bj.swiyu.trust.management.test;

import ch.admin.bj.swiyu.trust.management.modules.management.domain.*;
import ch.admin.bj.swiyu.trust.management.modules.management.domain.domainevent.DomainEventLogRepository;
import ch.admin.bj.swiyu.trust.management.modules.management.domain.task.ProtectedVerificationRequestTaskRepository;
import ch.admin.bj.swiyu.trust.management.modules.management.domain.task.TaskRepository;
import ch.admin.bj.swiyu.trust.management.modules.management.domain.task.TrustAddDidTaskRepository;
import ch.admin.bj.swiyu.trust.management.modules.management.domain.task.TrustOnboardingTaskRepository;
import ch.admin.bj.swiyu.trust.management.modules.registry.domain.NonComplianceListRepository;
import ch.admin.bj.swiyu.trust.management.modules.registry.domain.StatementRepository;
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
    public final BusinessPartnerIdentityRepository businessPartnerIdentity;
    public final ProtectedVerificationAuthorizationRepository protectedVerification;
    public final StatementRepository statement;
    public final TaskRepository task;
    public final ProtectedVerificationRequestTaskRepository protectedVerificationRequestTask;
    public final ProtectedVerificationAuthorizationRepository protectedVerificationAuthorization;
    public final TrustOnboardingTaskRepository trustOnboardingTask;
    public final TrustAddDidTaskRepository trustAddDidTask;

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
