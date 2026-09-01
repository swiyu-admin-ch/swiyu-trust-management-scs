package ch.admin.bj.swiyu.trust.management.modules.management.service;

import static ch.admin.bj.swiyu.trust.management.modules.management.domain.domainevent.DomainEventType.*;

import ch.admin.bj.swiyu.trust.management.modules.management.api.DomainEventLogDto;
import ch.admin.bj.swiyu.trust.management.modules.management.domain.domainevent.DomainEventLog;
import ch.admin.bj.swiyu.trust.management.modules.management.domain.domainevent.DomainEventLogRepository;
import ch.admin.bj.swiyu.trust.management.modules.management.domain.domainevent.QDomainEventLog;
import com.querydsl.core.BooleanBuilder;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Service for event internal to the TMS.
 */
@Service
@RequiredArgsConstructor
public class DomainEventService {

    private final DomainEventLogRepository domainEventLogRepository;

    @Transactional
    public void trustOnboardingSubmissionReceived(UUID taskId, String triggeredBy) {
        domainEventLogRepository.save(
            DomainEventLog.createTaskDomainEventLog(TRUST_ONBOARDING_SUBMISSION_RECEIVED, triggeredBy, taskId)
        );
    }

    @Transactional
    public void trustOnboardingSubmissionSucceeded(
        UUID taskId,
        String triggeredBy,
        String partnerNote,
        String internalNote
    ) {
        domainEventLogRepository.save(
            DomainEventLog.createTaskDomainEventLog(
                TRUST_ONBOARDING_SUCCEEDED,
                triggeredBy,
                taskId,
                partnerNote,
                internalNote
            )
        );
    }

    @Transactional
    public void trustOnboardingSubmissionRejected(
        UUID taskId,
        String triggeredBy,
        String partnerNote,
        String internalNote
    ) {
        domainEventLogRepository.save(
            DomainEventLog.createTaskDomainEventLog(
                TRUST_ONBOARDING_REJECTED,
                triggeredBy,
                taskId,
                partnerNote,
                internalNote
            )
        );
    }

    @Transactional
    public void trustOnboardingSubmissionMoreInformationRequested(
        UUID taskId,
        String triggeredBy,
        String partnerNote,
        String internalNote
    ) {
        domainEventLogRepository.save(
            DomainEventLog.createTaskDomainEventLog(
                TRUST_ONBOARDING_MORE_INFORMATION_REQUESTED,
                triggeredBy,
                taskId,
                partnerNote,
                internalNote
            )
        );
    }

    @Transactional
    public void trustOnboardingSubmissionResubmitted(UUID taskId, String triggeredBy) {
        domainEventLogRepository.save(
            DomainEventLog.createTaskDomainEventLog(TRUST_ONBOARDING_RESUBMITTED, triggeredBy, taskId)
        );
    }

    @Transactional
    public void taskNoteAdded(UUID taskId, String triggeredBy, String internalNote) {
        domainEventLogRepository.save(
            DomainEventLog.createTaskDomainEventLog(
                TASK_NOTE_ADDED,
                triggeredBy,
                taskId,
                null /* no partner note */,
                internalNote
            )
        );
    }

    @Transactional
    public void taskAssigned(UUID taskId, String triggeredBy) {
        domainEventLogRepository.save(DomainEventLog.createTaskDomainEventLog(TASK_ASSIGNED, triggeredBy, taskId));
    }

    @Transactional
    public void trustAddDidSubmissionReceived(UUID taskId, String triggeredBy) {
        domainEventLogRepository.save(
            DomainEventLog.createTaskDomainEventLog(TRUST_ADD_DID_SUBMISSION_RECEIVED, triggeredBy, taskId)
        );
    }

    @Transactional
    public void trustAddDidSubmissionSucceeded(UUID taskId, String triggeredBy) {
        domainEventLogRepository.save(
            DomainEventLog.createTaskDomainEventLog(TRUST_ADD_DID_SUCCEEDED, triggeredBy, taskId)
        );
    }

    @Transactional
    public void trustAddDidSubmissionRejected(UUID taskId, String triggeredBy, String internalNote) {
        domainEventLogRepository.save(
            DomainEventLog.createTaskDomainEventLog(TRUST_ADD_DID_REJECTED, triggeredBy, taskId, null, internalNote)
        );
    }

    @Transactional
    public void protectedVerificationRequestReceived(UUID taskId, String triggeredBy) {
        domainEventLogRepository.save(
            DomainEventLog.createTaskDomainEventLog(PROTECTED_VERIFICATION_REQUEST_RECEIVED, triggeredBy, taskId)
        );
    }

    @Transactional
    public void protectedVerificationRequestApproved(UUID taskId, String triggeredBy, String internalNote) {
        domainEventLogRepository.save(
            DomainEventLog.createTaskDomainEventLog(
                PROTECTED_VERIFICATION_REQUEST_APPROVED,
                triggeredBy,
                taskId,
                null,
                internalNote
            )
        );
    }

    @Transactional
    public void protectedVerificationRequestRejected(
        UUID taskId,
        String triggeredBy,
        String rejectReason,
        String internalNote
    ) {
        domainEventLogRepository.save(
            DomainEventLog.createTaskDomainEventLog(
                PROTECTED_VERIFICATION_REQUEST_REJECTED,
                triggeredBy,
                taskId,
                rejectReason,
                internalNote
            )
        );
    }

    @Transactional
    public void nonCompliantActorAdded(UUID nonCompliantActorId, String triggeredBy) {
        domainEventLogRepository.save(
            DomainEventLog.createNonCompliantActorDomainEventLog(
                NON_COMPLIANT_ACTOR_ADDED,
                triggeredBy,
                nonCompliantActorId
            )
        );
    }

    @Transactional
    public void nonCompliantActorRemoved(UUID nonCompliantActorId, String triggeredBy) {
        domainEventLogRepository.save(
            DomainEventLog.createNonCompliantActorDomainEventLog(
                NON_COMPLIANT_ACTOR_REMOVED,
                triggeredBy,
                nonCompliantActorId
            )
        );
    }

    public void protectedVerificationAuthorizationAdded(UUID pvaId, String triggeredBy) {
        domainEventLogRepository.save(
            DomainEventLog.createProtectedVerificationAuthorizationDomainEventLog(
                PROTECTED_VERIFICATION_AUTHORIZATION_ADDED,
                triggeredBy,
                pvaId
            )
        );
    }

    @Transactional
    public void protectedIssuanceEntryAdded(UUID protectedIssuanceEntryId, String triggeredBy) {
        domainEventLogRepository.save(
            DomainEventLog.createProtectedIssuanceEntryDomainEventLog(
                PROTECTED_ISSUANCE_ENTRY_ADDED,
                triggeredBy,
                protectedIssuanceEntryId
            )
        );
    }

    @Transactional
    public void protectedIssuanceEntryRemoved(UUID protectedIssuanceEntryId, String triggeredBy) {
        domainEventLogRepository.save(
            DomainEventLog.createProtectedIssuanceEntryDomainEventLog(
                PROTECTED_ISSUANCE_ENTRY_REMOVED,
                triggeredBy,
                protectedIssuanceEntryId
            )
        );
    }

    @Transactional
    public void protectedIssuanceAuthorizationAdded(
        UUID protectedIssuanceAuthorizationId,
        UUID businessPartnerId,
        String triggeredBy
    ) {
        domainEventLogRepository.save(
            DomainEventLog.createProtectedIssuanceAuthorizationDomainEventLog(
                PROTECTED_ISSUANCE_AUTHORIZATION_ADDED,
                triggeredBy,
                protectedIssuanceAuthorizationId,
                businessPartnerId
            )
        );
    }

    @Transactional
    public void protectedIssuanceAuthorizationRemoved(
        UUID protectedIssuanceAuthorizationId,
        UUID businessPartnerId,
        String triggeredBy
    ) {
        domainEventLogRepository.save(
            DomainEventLog.createProtectedIssuanceAuthorizationDomainEventLog(
                PROTECTED_ISSUANCE_AUTHORIZATION_REMOVED,
                triggeredBy,
                protectedIssuanceAuthorizationId,
                businessPartnerId
            )
        );
    }

    @Transactional(readOnly = true)
    public Page<DomainEventLogDto> getDomainEventLogs(UUID taskId, Pageable pageable) {
        QDomainEventLog d = QDomainEventLog.domainEventLog;

        BooleanBuilder where = new BooleanBuilder();
        if (taskId != null) {
            where.and(d.taskId.eq(taskId));
        }

        return this.domainEventLogRepository.findAll(where, pageable).map(DomainEventMapper::toDomainEventLogDto);
    }
}
