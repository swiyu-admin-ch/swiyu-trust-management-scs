package ch.admin.bj.swiyu.trust.management.modules.management.domain;

import static ch.admin.bj.swiyu.trust.management.test.TrustOnboardingSubmissionTestData.trustOnboardingSubmission;
import static ch.admin.bj.swiyu.trust.management.test.TrustOnboardingSubmissionTestData.trustOnboardingSubmissionWithoutContactPerson;
import static org.assertj.core.api.Assertions.assertThat;

import ch.admin.bj.swiyu.trust.client.core.business.internal.model.LanguageDto;
import org.junit.jupiter.api.Test;

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
}
