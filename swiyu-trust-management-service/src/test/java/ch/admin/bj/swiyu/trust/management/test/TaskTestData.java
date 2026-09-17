package ch.admin.bj.swiyu.trust.management.test;

import static ch.admin.bj.swiyu.trust.management.test.BusinessPartnerIdentityTestData.BUSINESS_PARTNER_NAME;
import static ch.admin.bj.swiyu.trust.management.test.TrustOnboardingTestData.ZONE_ID_ZURICH;
import static org.springframework.util.StringUtils.hasText;

import ch.admin.bj.swiyu.messagetype.ti.*;
import ch.admin.bj.swiyu.trust.client.core.business.internal.model.*;
import ch.admin.bj.swiyu.trust.management.modules.management.domain.task.ProtectedVerificationRequestTask;
import ch.admin.bj.swiyu.trust.management.modules.management.domain.task.TaskStatus;
import ch.admin.bj.swiyu.trust.management.modules.management.domain.task.TrustOnboardingTask;
import java.time.Instant;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.UUID;
import lombok.experimental.UtilityClass;

@UtilityClass
public class TaskTestData {

    public static ProtectedVerificationRequestTask protectedVerificationRequestTask() {
        return new ProtectedVerificationRequestTask(
            UUID.randomUUID(),
            BUSINESS_PARTNER_NAME,
            UUID.randomUUID(),
            Instant.now(),
            Instant.now().plus(12, ChronoUnit.DAYS)
        );
    }

    public static TrustOnboardingTask trustOnboardingTask() {
        var submittedAt = LocalDate.of(2025, 8, 9).atStartOfDay(ZONE_ID_ZURICH).toInstant();
        return TrustOnboardingTask.createRegistrationTask(
            UUID.randomUUID(),
            UUID.randomUUID(),
            BUSINESS_PARTNER_NAME,
            UUID.randomUUID(),
            submittedAt.plus(12, ChronoUnit.DAYS),
            submittedAt
        );
    }

    public static TrustOnboardingTask trustOnboardingTask(TaskStatus status, String assignee) {
        var submittedAt = LocalDate.of(2025, 8, 9).atStartOfDay(ZONE_ID_ZURICH).toInstant();
        var task = TrustOnboardingTask.createRegistrationTask(
            UUID.randomUUID(),
            UUID.randomUUID(),
            BUSINESS_PARTNER_NAME,
            UUID.randomUUID(),
            submittedAt.plus(12, ChronoUnit.DAYS),
            submittedAt
        );
        if (hasText(assignee)) {
            task.assignTo(assignee);
        }
        switch (status) {
            case REJECTED -> task.reject();
            case ACCEPTED -> task.approve();
            case INFORMATION_REQUESTED -> task.requestMoreInformation(Instant.now());
            case RESUBMITTED -> {
                task.requestMoreInformation(Instant.now());
                task.resubmit(Instant.now());
            }
            case OPENED -> {
                // nothing to do, default state
            }
        }
        return task;
    }

    public static TrustOnboardingTask trustOnboardingTask(Instant submittedAt) {
        return TrustOnboardingTask.createRegistrationTask(
            UUID.randomUUID(),
            UUID.randomUUID(),
            BUSINESS_PARTNER_NAME,
            UUID.randomUUID(),
            LocalDate.now().atStartOfDay(ZONE_ID_ZURICH).toInstant().plus(12, ChronoUnit.DAYS),
            submittedAt
        );
    }

    public static TrustOnboardingTask trustOnboardingTask(UUID id, Instant dueAt) {
        return TrustOnboardingTask.createRegistrationTask(
            id,
            UUID.randomUUID(),
            BUSINESS_PARTNER_NAME,
            UUID.randomUUID(),
            dueAt,
            LocalDate.now().atStartOfDay(ZONE_ID_ZURICH).toInstant().minus(12, ChronoUnit.DAYS)
        );
    }
}
