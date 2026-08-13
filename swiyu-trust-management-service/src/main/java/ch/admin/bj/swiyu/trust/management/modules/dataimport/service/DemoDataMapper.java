package ch.admin.bj.swiyu.trust.management.modules.dataimport.service;

import ch.admin.bj.swiyu.messagetype.ti.BusinessPartnerIdentityStatus;
import ch.admin.bj.swiyu.trust.management.modules.dataimport.domain.DemoData;
import ch.admin.bj.swiyu.trust.management.modules.management.domain.*;
import jakarta.validation.constraints.NotNull;
import java.time.Instant;
import java.time.ZoneId;
import java.time.temporal.ChronoUnit;
import lombok.experimental.UtilityClass;

@SuppressWarnings({ "java:S1854" })
@UtilityClass
public class DemoDataMapper {

    public static TrustOnboardingTask toTrustOnboardingTask(
        DemoData.DemoBusinessPartner bp,
        DemoData.DemoBusinessPartner.DemoTrustOnboarding onboarding
    ) {
        return new TrustOnboardingTask(
            onboarding.task().id(),
            bp.id(),
            bp.names(),
            onboarding.submissionId(),
            onboarding.task().dueAt(),
            onboarding.task().submittedAt()
        );
    }

    public static BusinessPartnerIdentity toBusinessPartnerIdentity(DemoData.DemoBusinessPartner bp) {
        var lastActivated = Instant.now().minus(10, ChronoUnit.DAYS);
        return new BusinessPartnerIdentity(
            bp.id(),
            bp.names(),
            lastActivated,
            bp.uid(),
            bp.uid() != null,
            bp.contact() == null ? "de" : bp.contact().correspondingLanguage().toString(),
            DemoDataMapper.toBusinessPartnerIdentityStatus(bp.bpi().status()),
            bp.type() == DemoData.DemoBusinessPartner.DemoBusinessPartnerType.GOVERNMENTAL_INSTITUTION,
            lastActivated.atZone(ZoneId.of("Europe/Zurich")).plusYears(3).toInstant(),
            lastActivated
        );
    }

    public static ProtectedVerificationAuthorization toProtectedVerificationAuthorization(
        DemoData.@NotNull DemoBusinessPartner bp,
        DemoData.DemoBusinessPartner.DemoBusinessPartnerIdentity.DemoProtectedVerificationAuthorization demoPva
    ) {
        return new ProtectedVerificationAuthorization(
            demoPva.id(),
            bp.id(),
            DemoDataMapper.toProtectedVerificationField(demoPva.field())
        );
    }

    public static TrustTaskStatus toTrustTaskStatus(
        DemoData.DemoBusinessPartner.DemoTrustOnboarding.DemoTrustOnboardingTask.@NotNull DemoTrustTaskStatus status
    ) {
        return switch (status) {
            case INFORMATION_REQUESTED -> TrustTaskStatus.INFORMATION_REQUESTED;
            case OPENED -> TrustTaskStatus.OPENED;
            case ACCEPTED -> TrustTaskStatus.ACCEPTED;
            case REJECTED -> TrustTaskStatus.REJECTED;
            case RESUBMITTED -> TrustTaskStatus.RESUBMITTED;
        };
    }

    private static ProtectedVerificationField toProtectedVerificationField(
        DemoData.DemoBusinessPartner.DemoBusinessPartnerIdentity.DemoProtectedVerificationAuthorization.DemoProtectedVerificationField field
    ) {
        return switch (field) {
            case AHV_NUMBER -> ProtectedVerificationField.AHV_NUMBER;
        };
    }

    private static @NotNull BusinessPartnerIdentityStatus toBusinessPartnerIdentityStatus(
        @NotNull DemoData.DemoBusinessPartner.DemoBusinessPartnerIdentity.DemoBusinessPartnerIdentityStatus status
    ) {
        return switch (status) {
            case ACTIVE -> BusinessPartnerIdentityStatus.ACTIVE;
            case DEACTIVATED -> BusinessPartnerIdentityStatus.DEACTIVATED;
        };
    }
}
