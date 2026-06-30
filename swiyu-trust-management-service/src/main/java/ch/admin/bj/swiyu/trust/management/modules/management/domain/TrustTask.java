package ch.admin.bj.swiyu.trust.management.modules.management.domain;

import static ch.admin.bj.swiyu.trust.management.modules.management.domain.TrustTaskStatusValidator.*;

import ch.admin.bj.swiyu.trust.management.modules.common.audit.*;
import ch.admin.bj.swiyu.trust.management.modules.common.i18n.ValidLocalizedMap;
import jakarta.persistence.*;
import jakarta.validation.constraints.*;
import java.time.*;
import java.util.*;
import lombok.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;
import org.springframework.data.jpa.domain.support.*;

@Entity
@EntityListeners(AuditingEntityListener.class)
@Getter
@Table(name = "trust_task")
@Inheritance(strategy = InheritanceType.JOINED)
@DiscriminatorColumn(name = "task_type")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public abstract class TrustTask {

    @Embedded
    private final AuditMetadata audit = new AuditMetadata();

    @Id
    private UUID id;

    @NotNull
    @Version
    private Long version;

    private UUID partnerId;

    @NotNull
    @ValidLocalizedMap
    @Column(name = "partner_name", columnDefinition = "jsonb")
    @JdbcTypeCode(SqlTypes.JSON)
    private Map<String, @NotBlank String> partnerName;

    @NotNull
    private Instant dueAt;

    @NotNull
    private Instant submittedAt;

    @NotNull
    @Enumerated(EnumType.STRING)
    private TrustTaskStatus status;

    private String assignee;

    @Column(name = "task_type", insertable = false, updatable = false)
    @Enumerated(EnumType.STRING)
    private TrustTaskType taskType;

    protected TrustTask(
        UUID id,
        UUID partnerId,
        Map<String, String> partnerName,
        Instant dueAt,
        Instant submittedAt,
        TrustTaskType taskType
    ) {
        this.id = id;
        this.partnerId = partnerId;
        this.partnerName = partnerName;
        this.dueAt = dueAt;
        this.submittedAt = submittedAt;
        this.status = TrustTaskStatus.OPENED;
        this.taskType = taskType;
    }

    public void assignTo(String assignee) {
        this.assignee = assignee;
    }

    public void changeStatus(TrustTaskStatus newStatus) {
        validateNewStatus(this.status, newStatus);
        this.status = newStatus;
    }

    protected void overwriteBaseFields(TrustTask source) {
        this.partnerId = source.partnerId;
        this.partnerName = source.partnerName;
        this.dueAt = source.dueAt;
        this.submittedAt = source.submittedAt;
        this.status = source.status;
    }
}
