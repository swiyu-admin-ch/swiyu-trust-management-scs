package ch.admin.bj.swiyu.trust.management.modules.management.domain.task;

import static ch.admin.bj.swiyu.trust.management.modules.management.domain.task.TaskStatusValidator.*;

import ch.admin.bj.swiyu.trust.management.modules.common.audit.*;
import ch.admin.bj.swiyu.trust.management.modules.common.i18n.ValidLocalizedMap;
import com.google.common.annotations.VisibleForTesting;
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
@Table(name = "task")
@Inheritance(strategy = InheritanceType.JOINED)
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public abstract class Task {

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

    private Instant dueAt;

    @NotNull
    private Instant submittedAt;

    @NotNull
    @Enumerated(EnumType.STRING)
    private TaskStatus status;

    private String assignee;

    @NotNull
    @Column(name = "task_type")
    @Enumerated(EnumType.STRING)
    private TaskType taskType;

    protected Task(
        UUID id,
        UUID partnerId,
        Map<String, String> partnerName,
        Instant dueAt,
        Instant submittedAt,
        TaskType taskType
    ) {
        this.id = id;
        this.partnerId = partnerId;
        this.partnerName = partnerName;
        this.dueAt = dueAt;
        this.submittedAt = submittedAt;
        this.status = TaskStatus.OPENED;
        this.taskType = taskType;
    }

    public void assignTo(String assignee) {
        this.assignee = assignee;
    }

    public void approve() {
        changeStatus(TaskStatus.ACCEPTED);
        this.dueAt = null;
    }

    public void reject() {
        changeStatus(TaskStatus.REJECTED);
        this.dueAt = null;
    }

    @VisibleForTesting
    public void overrideDueAt(Instant dueAt) {
        this.dueAt = dueAt;
    }

    protected void requestMoreInformation() {
        changeStatus(TaskStatus.INFORMATION_REQUESTED);
        this.dueAt = null;
    }

    protected void resubmit(Instant dueAt) {
        changeStatus(TaskStatus.RESUBMITTED);
        this.dueAt = dueAt;
    }

    private void changeStatus(TaskStatus newStatus) {
        validateNewStatus(this.status, newStatus);
        this.status = newStatus;
    }

    /**
     * Utility method for simple demo data updates.
     */
    protected void overwriteForDemoData(Task source) {
        this.partnerId = source.partnerId;
        this.partnerName = source.partnerName;
        this.dueAt = source.dueAt;
        this.submittedAt = source.submittedAt;
        this.status = source.status;
        this.taskType = source.taskType;
    }
}
