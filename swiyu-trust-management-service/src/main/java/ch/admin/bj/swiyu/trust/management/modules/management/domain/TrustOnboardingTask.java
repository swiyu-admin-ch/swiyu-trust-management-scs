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

    @NotNull
    private UUID trustOnboardingSubmissionId; // id from swiyu-core-business-service

    public TrustOnboardingTask(
        UUID partnerId,
        PartnerName partnerName,
        UUID trustOnboardingSubmissionId,
        Instant dueAt,
        Instant submittedAt
    ) {
        this(UUID.randomUUID(), partnerId, partnerName, trustOnboardingSubmissionId, dueAt, submittedAt);
    }

    public TrustOnboardingTask(
        UUID id,
        UUID partnerId,
        PartnerName partnerName,
        UUID trustOnboardingSubmissionId,
        Instant dueAt,
        Instant submittedAt
    ) {
        super(id, partnerId, partnerName, dueAt, submittedAt, TrustTaskType.ONBOARDING);
        this.trustOnboardingSubmissionId = trustOnboardingSubmissionId;
    }

    @VisibleForTesting
    public void overwriteFrom(TrustOnboardingTask source) {
        overwriteBaseFields(source);
        this.trustOnboardingSubmissionId = source.trustOnboardingSubmissionId;
    }
}
