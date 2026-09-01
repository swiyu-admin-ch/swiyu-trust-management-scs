package ch.admin.bj.swiyu.trust.management.modules.management.domain.task;

import jakarta.persistence.*;
import jakarta.validation.constraints.*;
import java.time.*;
import java.util.*;
import lombok.*;

@Entity
@Getter
@Table(name = "trust_add_did_task")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class TrustAddDidTask extends Task {

    @NotNull
    private UUID trustAddDidSubmissionId;

    @NotNull
    private String permissionDid;

    public TrustAddDidTask(
        UUID partnerId,
        Map<String, String> partnerName,
        UUID trustAddDidSubmissionId,
        String permissionDid,
        Instant dueAt,
        Instant submittedAt
    ) {
        super(UUID.randomUUID(), partnerId, partnerName, dueAt, submittedAt, TaskType.ADD_DID);
        this.trustAddDidSubmissionId = trustAddDidSubmissionId;
        this.permissionDid = permissionDid;
    }
}
