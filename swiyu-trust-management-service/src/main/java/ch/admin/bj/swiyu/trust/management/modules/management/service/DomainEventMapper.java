package ch.admin.bj.swiyu.trust.management.modules.management.service;

import ch.admin.bj.swiyu.trust.management.modules.management.api.DomainEventLogDto;
import ch.admin.bj.swiyu.trust.management.modules.management.api.DomainEventTypeDto;
import ch.admin.bj.swiyu.trust.management.modules.management.domain.domainevent.DomainEventLog;
import ch.admin.bj.swiyu.trust.management.modules.management.domain.domainevent.DomainEventType;
import lombok.experimental.UtilityClass;

@UtilityClass
public class DomainEventMapper {

    public DomainEventLogDto toDomainEventLogDto(DomainEventLog source) {
        return new DomainEventLogDto(
            source.getTriggeredAt(),
            source.getTriggeredBy(),
            toDomainEventTypeDto(source.getEventType()),
            source.getPartnerNote(),
            source.getInternalNote()
        );
    }

    private static DomainEventTypeDto toDomainEventTypeDto(DomainEventType source) {
        return switch (source) {
            case TRUST_ONBOARDING_SUBMISSION_RECEIVED -> DomainEventTypeDto.TRUST_ONBOARDING_SUBMISSION_RECEIVED;
            case TRUST_ONBOARDING_SUCCEEDED -> DomainEventTypeDto.TRUST_ONBOARDING_SUCCEEDED;
            case TRUST_ONBOARDING_REJECTED -> DomainEventTypeDto.TRUST_ONBOARDING_REJECTED;
            case TRUST_ONBOARDING_MORE_INFORMATION_REQUESTED -> DomainEventTypeDto.TRUST_ONBOARDING_MORE_INFORMATION_REQUESTED;
            case TRUST_ONBOARDING_TASK_NOTE_ADDED -> DomainEventTypeDto.TRUST_ONBOARDING_TASK_NOTE_ADDED;
            case TRUST_ONBOARDING_TASK_ASSIGNED -> DomainEventTypeDto.TRUST_ONBOARDING_TASK_ASSIGNED;
            case TRUST_ADD_DID_SUBMISSION_RECEIVED -> DomainEventTypeDto.TRUST_ADD_DID_SUBMISSION_RECEIVED;
            case TRUST_ADD_DID_SUCCEEDED -> DomainEventTypeDto.TRUST_ADD_DID_SUCCEEDED;
            case TRUST_ADD_DID_REJECTED -> DomainEventTypeDto.TRUST_ADD_DID_REJECTED;
            case NON_COMPLIANT_ACTOR_ADDED -> DomainEventTypeDto.NON_COMPLIANT_ACTOR_ADDED;
            case NON_COMPLIANT_ACTOR_REMOVED -> DomainEventTypeDto.NON_COMPLIANT_ACTOR_REMOVED;
            case PROTECTED_ISSUANCE_ENTRY_ADDED -> DomainEventTypeDto.PROTECTED_ISSUANCE_ENTRY_ADDED;
            case PROTECTED_ISSUANCE_ENTRY_REMOVED -> DomainEventTypeDto.PROTECTED_ISSUANCE_ENTRY_REMOVED;
        };
    }
}
