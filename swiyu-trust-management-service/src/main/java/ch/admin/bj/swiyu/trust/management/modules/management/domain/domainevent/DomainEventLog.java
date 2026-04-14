package ch.admin.bj.swiyu.trust.management.modules.management.domain.domainevent;

import ch.admin.bj.swiyu.trust.management.modules.common.audit.AuditMetadata;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
import java.time.Instant;
import java.util.UUID;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

@Entity
@EntityListeners(AuditingEntityListener.class)
@Getter
@Table(name = "domain_event_log")
@NoArgsConstructor
public class DomainEventLog {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Embedded
    @NotNull
    private final AuditMetadata auditMetadata = new AuditMetadata();

    @Enumerated(EnumType.STRING)
    @NotNull
    private DomainEventType eventType;

    @NotNull
    private Instant triggeredAt;

    /**
     * The full name of the user who triggered the event, e.g. "John Doe".
     */
    @NotNull
    private String triggeredBy;

    private UUID trustTaskId;

    private UUID nonCompliantActorId;

    private String partnerNote;

    private String internalNote;

    public static DomainEventLog createTrustTaskDomainEventLog(
        DomainEventType eventType,
        String triggeredBy,
        UUID trustTaskId
    ) {
        var domainEventLog = new DomainEventLog();
        domainEventLog.eventType = eventType;
        domainEventLog.triggeredAt = Instant.now();
        domainEventLog.triggeredBy = triggeredBy;
        domainEventLog.trustTaskId = trustTaskId;
        return domainEventLog;
    }

    public static DomainEventLog createTrustTaskDomainEventLog(
        DomainEventType eventType,
        String triggeredBy,
        UUID trustTaskId,
        String partnerNote,
        String internalNote
    ) {
        var domainEventLog = new DomainEventLog();
        domainEventLog.eventType = eventType;
        domainEventLog.triggeredAt = Instant.now();
        domainEventLog.triggeredBy = triggeredBy;
        domainEventLog.trustTaskId = trustTaskId;
        domainEventLog.partnerNote = partnerNote;
        domainEventLog.internalNote = internalNote;
        return domainEventLog;
    }

    public static DomainEventLog createNonCompliantActorDomainEventLog(
        DomainEventType eventType,
        String triggeredBy,
        UUID nonCompliantActorId
    ) {
        var domainEventLog = new DomainEventLog();
        domainEventLog.eventType = eventType;
        domainEventLog.triggeredAt = Instant.now();
        domainEventLog.triggeredBy = triggeredBy;
        domainEventLog.nonCompliantActorId = nonCompliantActorId;
        return domainEventLog;
    }
}
