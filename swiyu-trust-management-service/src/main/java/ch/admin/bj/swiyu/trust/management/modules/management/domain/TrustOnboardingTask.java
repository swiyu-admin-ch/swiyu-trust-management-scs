package ch.admin.bj.swiyu.trust.management.modules.management.domain;

import com.google.common.annotations.*;
import jakarta.persistence.*;
import jakarta.validation.constraints.*;
import java.time.*;
import java.util.*;
import lombok.*;

@Entity
@Getter
@Table(name = "trust_onboarding_task")
@DiscriminatorValue("ONBOARDING")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class TrustOnboardingTask extends TrustTask {

    // Partner may resubmit at most twice after being asked for more information; a 3rd request is rejected instead.
    private static final int MAX_RESUBMISSIONS = 2;

    @NotNull
    private UUID trustOnboardingSubmissionId; // id from swiyu-core-business-service

    private Instant rejectionEnforcedAt;

    private int timesResubmitted;

    public TrustOnboardingTask(
        UUID partnerId,
        Map<String, String> partnerName,
        UUID trustOnboardingSubmissionId,
        Instant dueAt,
        Instant submittedAt
    ) {
        this(UUID.randomUUID(), partnerId, partnerName, trustOnboardingSubmissionId, dueAt, submittedAt);
    }

    public TrustOnboardingTask(
        UUID id,
        UUID partnerId,
        Map<String, String> partnerName,
        UUID trustOnboardingSubmissionId,
        Instant dueAt,
        Instant submittedAt
    ) {
        super(id, partnerId, partnerName, dueAt, submittedAt, TrustTaskType.ONBOARDING);
        this.trustOnboardingSubmissionId = trustOnboardingSubmissionId;
        this.timesResubmitted = 0;
    }

    public boolean canRequestMoreInformation() {
        return timesResubmitted < MAX_RESUBMISSIONS;
    }

    public void requestMoreInformation(Instant rejectionEnforcedAt) {
        changeStatus(TrustTaskStatus.INFORMATION_REQUESTED);
        setDueAt(null);
        this.rejectionEnforcedAt = rejectionEnforcedAt;
    }

    public void markResubmitted(Instant dueAt) {
        this.timesResubmitted++;
        changeStatus(TrustTaskStatus.RESUBMITTED);
        setDueAt(dueAt);
        this.rejectionEnforcedAt = null;
    }

    @VisibleForTesting
    public void overrideRejectionEnforcedAt(Instant rejectionEnforcedAt) {
        this.rejectionEnforcedAt = rejectionEnforcedAt;
    }

    @VisibleForTesting
    public void overwriteFrom(TrustOnboardingTask source) {
        overwriteBaseFields(source);
        this.trustOnboardingSubmissionId = source.trustOnboardingSubmissionId;
        this.rejectionEnforcedAt = source.rejectionEnforcedAt;
        this.timesResubmitted = source.timesResubmitted;
    }
}
