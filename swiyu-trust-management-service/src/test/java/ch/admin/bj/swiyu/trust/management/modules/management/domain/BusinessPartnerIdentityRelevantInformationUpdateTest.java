package ch.admin.bj.swiyu.trust.management.modules.management.domain;

import static ch.admin.bj.swiyu.trust.management.test.TrustOnboardingSubmissionTestData.trustOnboardingSubmission;
import static ch.admin.bj.swiyu.trust.management.test.TrustOnboardingSubmissionTestData.trustOnboardingSubmissionWithoutContactPerson;
import static org.assertj.core.api.Assertions.assertThat;

import ch.admin.bj.swiyu.trust.client.core.business.internal.model.LanguageDto;
import ch.admin.bj.swiyu.trust.client.core.business.internal.model.ProofOfPossessionDto;
import ch.admin.bj.swiyu.trust.client.core.business.internal.model.ProofOfPossessionStatusDto;
import ch.admin.bj.swiyu.trust.client.core.business.internal.model.TrustOnboardingSubmissionDto;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

class BusinessPartnerIdentityRelevantInformationUpdateTest {

    @Test
    void of_correspondingLanguageTakenFromContactPerson() {
        // Given - the language comes from contactPerson; the former top-level field of the
        // submission is no longer part of the CBS client contract
        var submission = trustOnboardingSubmission();
        submission.getContactPerson().setCorrespondingLanguage(LanguageDto.FR);

        // When
        var result = BusinessPartnerIdentityRelevantInformationUpdate.of(submission);

        // Then
        assertThat(result.correspondingLanguage()).isEqualTo(LanguageDto.FR.toString());
    }

    @Test
    void of_contactPersonNull_correspondingLanguageIsNull() {
        // Given - the CBS contract declares contactPerson as optional
        var submission = trustOnboardingSubmissionWithoutContactPerson();

        // When - must not throw NullPointerException
        var result = BusinessPartnerIdentityRelevantInformationUpdate.of(submission);

        // Then
        assertThat(result.correspondingLanguage()).isNull();
    }

    @Test
    void of_contactPersonWithoutLanguage_correspondingLanguageIsNull() {
        // Given
        var submission = trustOnboardingSubmission();
        submission.getContactPerson().setCorrespondingLanguage(null);

        // When
        var result = BusinessPartnerIdentityRelevantInformationUpdate.of(submission);

        // Then
        assertThat(result.correspondingLanguage()).isNull();
    }

    @Test
    void of_registration_takesOverIsRegisteredInCommercialRegister() {
        // Given - only a registration carries the flag, and there it is authoritative
        var submission = trustOnboardingSubmission();
        submission.setType(TrustOnboardingSubmissionDto.TypeEnum.REGISTRATION);
        submission.setIsRegisteredInCommercialRegister(true);

        // When
        var result = BusinessPartnerIdentityRelevantInformationUpdate.of(submission);

        // Then
        assertThat(result.isRegisteredInCommercialRegister()).isTrue();
    }

    @Test
    void of_renewal_leavesIsRegisteredInCommercialRegisterUntouched() {
        // Given - EIDARTFE-1301 lists the flag among the properties a renewal must not change, and the
        // CBS hardcodes it to false on the derived submission; taking that over would clear it on the
        // identity of a partner that is in fact registered
        var submission = trustOnboardingSubmission();
        submission.setType(TrustOnboardingSubmissionDto.TypeEnum.RENEWAL);
        submission.setIsRegisteredInCommercialRegister(false);

        // When
        var result = BusinessPartnerIdentityRelevantInformationUpdate.of(submission);

        // Then - null tells the identity to keep its current value
        assertThat(result.isRegisteredInCommercialRegister()).isNull();
    }

    @ParameterizedTest
    @EnumSource(
        value = TrustOnboardingSubmissionDto.TypeEnum.class,
        names = { "PROFILE_CHANGE_MANDATORY", "PROFILE_CHANGE_VOLUNTARY" }
    )
    void of_profileChange_takesOverIsRegisteredInCommercialRegister(TrustOnboardingSubmissionDto.TypeEnum type) {
        // Given - a profile change may change the flag (EIDARTFE-1301)
        var submission = trustOnboardingSubmission();
        submission.setType(type);
        submission.setIsRegisteredInCommercialRegister(true);

        // When
        var result = BusinessPartnerIdentityRelevantInformationUpdate.of(submission);

        // Then
        assertThat(result.isRegisteredInCommercialRegister()).isTrue();
    }

    @Test
    void of_onlyProvenProofOfPossessionsBecomeTrustedIdentifiers() {
        // Given - a derived submission lists the already trusted DIDs as NOT_SUPPLIED until the partner
        // proves possession again
        var submission = trustOnboardingSubmission();
        submission.setType(TrustOnboardingSubmissionDto.TypeEnum.RENEWAL);
        submission.setProofOfPossessions(
            List.of(
                pop("did:proven", ProofOfPossessionStatusDto.VALID),
                pop("did:unproven", ProofOfPossessionStatusDto.NOT_SUPPLIED)
            )
        );

        // When
        var result = BusinessPartnerIdentityRelevantInformationUpdate.of(submission);

        // Then
        assertThat(result.trustedIdentifier()).containsExactly("did:proven");
    }

    private static ProofOfPossessionDto pop(String did, ProofOfPossessionStatusDto status) {
        var pop = new ProofOfPossessionDto();
        pop.setDid(did);
        pop.setNonce("nonce");
        pop.setStatus(status);
        return pop;
    }
}
