package ch.admin.bj.swiyu.trust.management.modules.management.domain.task;

import com.google.common.annotations.*;
import jakarta.persistence.*;
import jakarta.validation.constraints.*;
import java.time.*;
import java.util.*;
import lombok.*;

@Entity
@Getter
@Table(name = "trust_onboarding_task")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class TrustOnboardingTask extends Task {

    // Partner may resubmit at most twice after being asked for more information; a 3rd request is rejected instead.
    private static final int MAX_RESUBMISSIONS = 2;

    @NotNull
    private UUID trustOnboardingSubmissionId; // id from swiyu-core-business-service

    private Instant rejectionEnforcedAt;

    private int timesResubmitted;

    public static TrustOnboardingTask createRegistrationTask(
        UUID id,
        UUID partnerId,
        Map<String, String> partnerName,
        UUID trustOnboardingSubmissionId,
        Instant dueAt,
        Instant submittedAt
    ) {
        return new TrustOnboardingTask(
            id,
            partnerId,
            partnerName,
            trustOnboardingSubmissionId,
            TaskType.REGISTRATION,
            dueAt,
            submittedAt
        );
    }

    public static TrustOnboardingTask createRenewalTask(
        UUID id,
        UUID partnerId,
        Map<String, String> partnerName,
        UUID trustOnboardingSubmissionId,
        Instant dueAt,
        Instant submittedAt
    ) {
        return new TrustOnboardingTask(
            id,
            partnerId,
            partnerName,
            trustOnboardingSubmissionId,
            TaskType.RENEWAL,
            dueAt,
            submittedAt
        );
    }

    public static TrustOnboardingTask createProfileChangeTask(
        UUID id,
        UUID partnerId,
        Map<String, String> partnerName,
        UUID trustOnboardingSubmissionId,
        Instant dueAt,
        Instant submittedAt
    ) {
        return new TrustOnboardingTask(
            id,
            partnerId,
            partnerName,
            trustOnboardingSubmissionId,
            TaskType.PROFILE_CHANGE,
            dueAt,
            submittedAt
        );
    }

    // keep private, this task is only allowed to be of type REGISTRATION, RENEWAL or PROFILE_CHANGE
    private TrustOnboardingTask(
        UUID id,
        UUID partnerId,
        Map<String, String> partnerName,
        UUID trustOnboardingSubmissionId,
        TaskType taskType,
        Instant dueAt,
        Instant submittedAt
    ) {
        super(id, partnerId, partnerName, dueAt, submittedAt, validateTaskType(taskType));
        this.trustOnboardingSubmissionId = trustOnboardingSubmissionId;
        this.timesResubmitted = 0;
    }

    public boolean canRequestMoreInformation() {
        return timesResubmitted < MAX_RESUBMISSIONS;
    }

    public void requestMoreInformation(Instant rejectionEnforcedAt) {
        super.requestMoreInformation();
        this.rejectionEnforcedAt = rejectionEnforcedAt;
    }

    @Override
    public void resubmit(Instant dueAt) {
        super.resubmit(dueAt);
        this.timesResubmitted++;
        this.rejectionEnforcedAt = null;
    }

    @VisibleForTesting
    public void overrideRejectionEnforcedAt(Instant rejectionEnforcedAt) {
        this.rejectionEnforcedAt = rejectionEnforcedAt;
    }

    /**
     * Utility method for simple demo data updates.
     */
    public void overwriteForDemoData(TrustOnboardingTask source) {
        super.overwriteForDemoData(source);
        this.trustOnboardingSubmissionId = source.trustOnboardingSubmissionId;
        this.rejectionEnforcedAt = source.rejectionEnforcedAt;
        this.timesResubmitted = source.timesResubmitted;
    }

    private static TaskType validateTaskType(TaskType taskType) {
        if (!EnumSet.of(TaskType.REGISTRATION, TaskType.RENEWAL, TaskType.PROFILE_CHANGE).contains(taskType)) {
            throw new IllegalArgumentException(
                "TrustOnboardingTask can only be of type REGISTRATION, RENEWAL or PROFILE_CHANGE"
            );
        }
        return taskType;
    }
}
