package ch.admin.bj.swiyu.trust.management.modules.management.domain.task;

import jakarta.persistence.*;
import jakarta.validation.constraints.*;
import java.time.*;
import java.util.*;
import lombok.*;

@Entity
@Getter
@Table(name = "protected_verification_request_task")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class ProtectedVerificationRequestTask extends Task {

    @NotNull
    private UUID protectedVerificationSubmissionId; // id from swiyu-core-business-service

    // Set once the BJ user has opened (or refreshed) the ZAS data sub-section on the task detail page.
    // Approve/reject is blocked until this is set, since the decision must be made having seen live ZAS data.
    private Instant zasDataOpenedAt;

    public ProtectedVerificationRequestTask(
        UUID partnerId,
        Map<String, String> partnerName,
        UUID protectedVerificationSubmissionId,
        Instant dueAt,
        Instant submittedAt
    ) {
        super(UUID.randomUUID(), partnerId, partnerName, dueAt, submittedAt, TaskType.PROTECTED_VERIFICATION_REQUEST);
        this.protectedVerificationSubmissionId = protectedVerificationSubmissionId;
    }

    public void markZasDataOpened(Instant openedAt) {
        this.zasDataOpenedAt = openedAt;
    }

    public boolean hasOpenedZasData() {
        return zasDataOpenedAt != null;
    }
}
