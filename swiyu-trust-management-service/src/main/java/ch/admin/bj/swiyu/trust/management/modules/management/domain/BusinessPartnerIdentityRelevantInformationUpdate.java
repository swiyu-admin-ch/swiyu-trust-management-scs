package ch.admin.bj.swiyu.trust.management.modules.management.domain;

import ch.admin.bj.swiyu.trust.client.core.business.internal.model.BusinessPartnerTypeDto;
import ch.admin.bj.swiyu.trust.client.core.business.internal.model.ProofOfPossessionDto;
import ch.admin.bj.swiyu.trust.client.core.business.internal.model.ProofOfPossessionStatusDto;
import ch.admin.bj.swiyu.trust.client.core.business.internal.model.TrustOnboardingSubmissionDto;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * The part of a trust onboarding submission that shapes the business partner identity. A null field means "not
 * carried by this submission type" and keeps the stored value.
 */
public record BusinessPartnerIdentityRelevantInformationUpdate(
    Map<String, String> entityName,
    String uid,
    Boolean isRegisteredInCommercialRegister,
    String correspondingLanguage,
    Boolean isStateActor,
    @NotNull Set<@NotBlank String> trustedIdentifier
) {
    public static BusinessPartnerIdentityRelevantInformationUpdate of(TrustOnboardingSubmissionDto submission) {
        return new BusinessPartnerIdentityRelevantInformationUpdate(
            Map.copyOf(submission.getName()),
            submission.getRegistryIds().get("UID"),
            toIsRegisteredInCommercialRegister(submission),
            toCorrespondingLanguage(submission),
            submission.getBusinessPartnerType() == BusinessPartnerTypeDto.GOVERNMENTAL_INSTITUTION,
            toTrustedIdentifier(submission)
        );
    }

    /**
     * The commercial register entry this update sets: the submitted value when the submission carries one, the current
     * value otherwise. Both halves of that rule live here - {@link #toIsRegisteredInCommercialRegister} decides when
     * the field is absent, this method decides what absent means - so a caller just assigns the result.
     *
     * @param current what the identity holds today
     */
    public Boolean isRegisteredInCommercialRegisterOr(Boolean current) {
        return isRegisteredInCommercialRegister != null ? isRegisteredInCommercialRegister : current;
    }

    /**
     * A renewal carries no value for this flag (EIDARTFE-1301 lists it as not changeable), so null means unchanged
     * instead of false - otherwise a renewal would drop the commercial register entry. No caller unboxes the result:
     * {@link #isRegisteredInCommercialRegisterOr} resolves null against the stored value, and the registration path
     * reads it through {@code Boolean.TRUE.equals}.
     */
    @SuppressWarnings("java:S2447") // null is the documented carrier of "not communicated", see above
    private static Boolean toIsRegisteredInCommercialRegister(TrustOnboardingSubmissionDto submission) {
        if (submission.getType() == TrustOnboardingSubmissionDto.TypeEnum.RENEWAL) {
            return null;
        }
        return Boolean.TRUE.equals(submission.getIsRegisteredInCommercialRegister());
    }

    /**
     * Only DIDs whose possession was actually proven become trusted identifiers.
     */
    private static Set<String> toTrustedIdentifier(TrustOnboardingSubmissionDto submission) {
        return submission
            .getProofOfPossessions()
            .stream()
            .filter(pop -> pop.getStatus() == ProofOfPossessionStatusDto.VALID)
            .map(ProofOfPossessionDto::getDid)
            .collect(Collectors.toSet());
    }

    private static String toCorrespondingLanguage(TrustOnboardingSubmissionDto submission) {
        var contactPerson = submission.getContactPerson();
        if (contactPerson == null || contactPerson.getCorrespondingLanguage() == null) {
            return null;
        }
        return contactPerson.getCorrespondingLanguage().toString();
    }
}
