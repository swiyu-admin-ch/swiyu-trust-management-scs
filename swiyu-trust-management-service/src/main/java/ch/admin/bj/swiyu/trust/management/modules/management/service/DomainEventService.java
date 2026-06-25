package ch.admin.bj.swiyu.trust.management.modules.management.service;

import static ch.admin.bj.swiyu.trust.management.modules.management.domain.domainevent.DomainEventType.*;

import ch.admin.bj.swiyu.trust.management.modules.management.domain.domainevent.DomainEventLog;
import ch.admin.bj.swiyu.trust.management.modules.management.domain.domainevent.DomainEventLogRepository;
import ch.admin.bj.swiyu.trust.management.modules.management.domain.domainevent.QDomainEventLog;
import ch.admin.bj.swiyu.trust.management.modules.ui.api.DomainEventLogDto;
import com.querydsl.core.BooleanBuilder;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class DomainEventService {

    private final DomainEventLogRepository domainEventLogRepository;

    @Transactional
    public void trustOnboardingSubmissionReceived(UUID trustTaskId, String triggeredBy) {
        domainEventLogRepository.save(
            DomainEventLog.createTrustTaskDomainEventLog(TRUST_ONBOARDING_SUBMISSION_RECEIVED, triggeredBy, trustTaskId)
        );
    }

    @Transactional
    public void trustOnboardingSubmissionSucceeded(
        UUID trustTaskId,
        String triggeredBy,
        String partnerNote,
        String internalNote
    ) {
        domainEventLogRepository.save(
            DomainEventLog.createTrustTaskDomainEventLog(
                TRUST_ONBOARDING_SUCCEEDED,
                triggeredBy,
                trustTaskId,
                partnerNote,
                internalNote
            )
        );
    }

    @Transactional
    public void trustOnboardingSubmissionRejected(
        UUID trustTaskId,
        String triggeredBy,
        String partnerNote,
        String internalNote
    ) {
        domainEventLogRepository.save(
            DomainEventLog.createTrustTaskDomainEventLog(
                TRUST_ONBOARDING_REJECTED,
                triggeredBy,
                trustTaskId,
                partnerNote,
                internalNote
            )
        );
    }

    @Transactional
    public void trustOnboardingSubmissionMoreInformationRequested(
        UUID trustTaskId,
        String triggeredBy,
        String partnerNote,
        String internalNote
    ) {
        domainEventLogRepository.save(
            DomainEventLog.createTrustTaskDomainEventLog(
                TRUST_ONBOARDING_MORE_INFORMATION_REQUESTED,
                triggeredBy,
                trustTaskId,
                partnerNote,
                internalNote
            )
        );
    }

    @Transactional
    public void trustOnboardingSubmissionTaskNoteAdded(UUID trustTaskId, String triggeredBy, String internalNote) {
        domainEventLogRepository.save(
            DomainEventLog.createTrustTaskDomainEventLog(
                TRUST_ONBOARDING_TASK_NOTE_ADDED,
                triggeredBy,
                trustTaskId,
                null /* no partner note */,
                internalNote
            )
        );
    }

    @Transactional
    public void trustOnboardingSubmissionAssigned(UUID trustTaskId, String triggeredBy) {
        domainEventLogRepository.save(
            DomainEventLog.createTrustTaskDomainEventLog(TRUST_ONBOARDING_TASK_ASSIGNED, triggeredBy, trustTaskId)
        );
    }

    @Transactional
    public void trustAddDidSubmissionReceived(UUID trustTaskId, String triggeredBy) {
        domainEventLogRepository.save(
            DomainEventLog.createTrustTaskDomainEventLog(TRUST_ADD_DID_SUBMISSION_RECEIVED, triggeredBy, trustTaskId)
        );
    }

    @Transactional
    public void trustAddDidSubmissionSucceeded(UUID trustTaskId, String triggeredBy) {
        domainEventLogRepository.save(
            DomainEventLog.createTrustTaskDomainEventLog(TRUST_ADD_DID_SUCCEEDED, triggeredBy, trustTaskId)
        );
    }

    @Transactional
    public void trustAddDidSubmissionRejected(UUID trustTaskId, String triggeredBy, String internalNote) {
        domainEventLogRepository.save(
            DomainEventLog.createTrustTaskDomainEventLog(
                TRUST_ADD_DID_REJECTED,
                triggeredBy,
                trustTaskId,
                null,
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

    @Transactional(readOnly = true)
    public Page<DomainEventLogDto> getDomainEventLogs(UUID trustTaskId, Pageable pageable) {
        QDomainEventLog d = QDomainEventLog.domainEventLog;

        BooleanBuilder where = new BooleanBuilder();
        if (trustTaskId != null) {
            where.and(d.trustTaskId.eq(trustTaskId));
        }

        return this.domainEventLogRepository.findAll(where, pageable).map(DomainEventMapper::toDomainEventLogDto);
    }
}
