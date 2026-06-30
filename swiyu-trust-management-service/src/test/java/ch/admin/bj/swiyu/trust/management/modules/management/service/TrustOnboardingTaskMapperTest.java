package ch.admin.bj.swiyu.trust.management.modules.management.service;

import static ch.admin.bj.swiyu.trust.management.modules.management.service.TrustOnboardingTaskMapper.toTrustOnboardingTaskDto;
import static ch.admin.bj.swiyu.trust.management.modules.management.service.TrustOnboardingTaskMapper.toTrustOnboardingTaskListItemDto;
import static ch.admin.bj.swiyu.trust.management.test.TrustOnboardingSubmissionTestData.trustOnboardingSubmission;
import static ch.admin.bj.swiyu.trust.management.test.TrustOnboardingTestData.trustOnboardingTask;
import static org.assertj.core.api.Assertions.assertThat;

import ch.admin.bj.swiyu.trust.client.core.business.internal.model.LanguageDto;
import ch.admin.bj.swiyu.trust.client.core.business.internal.model.SignatoryDto;
import ch.admin.bj.swiyu.trust.management.modules.management.api.TrustOnboardingTaskActionDto;
import ch.admin.bj.swiyu.trust.management.modules.ui.api.BusinessPartnerTypeDto;
import ch.admin.bj.swiyu.trust.management.modules.ui.api.TrustOnboardingTaskContactTypeDto;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.junit.jupiter.api.Test;

class TrustOnboardingTaskMapperTest {

    @Test
    void toTrustOnboardingTaskListItemDtoTest() {
        // Given
        var allowedActions = Set.of(TrustOnboardingTaskActionDto.APPROVE);
        var task = trustOnboardingTask();

        // When
        var result = toTrustOnboardingTaskListItemDto(task, allowedActions);

        // Then
        assertThat(result).isNotNull();
    }

    @Test
    void toTrustOnboardingTaskDtoTest() {
        // Given
        var allowedActions = Set.of(TrustOnboardingTaskActionDto.APPROVE);
        var submission = trustOnboardingSubmission();
        var task = trustOnboardingTask();

        // When
        var result = toTrustOnboardingTaskDto(allowedActions, task, submission);

        // Then
        assertThat(result).isNotNull();
        assertThat(result.entityName())
            .containsEntry("de-CH", submission.getName().get("de-CH"))
            .containsEntry("default", "EntityDE");
        assertThat(result.partnerType()).isEqualTo(BusinessPartnerTypeDto.GOVERNMENTAL_INSTITUTION);
        assertThat(result.dids().getFirst().did()).isEqualTo("did:123");
        assertThat(result.submittedAt()).isEqualTo(task.getSubmittedAt());
    }

    @Test
    void toTrustOnboardingTaskDtoTest_registryIdWithNullValue_shouldReturnNullUid() {
        // Given - reproduces bug: "uid" key is present but its value is null.
        var allowedActions = Set.of(TrustOnboardingTaskActionDto.APPROVE);
        var submission = trustOnboardingSubmission();
        var registryIdsWithNullValue = new HashMap<String, String>();
        registryIdsWithNullValue.put("uid", null);
        submission.setRegistryIds(registryIdsWithNullValue);
        var task = trustOnboardingTask();

        // When
        var result = toTrustOnboardingTaskDto(allowedActions, task, submission);

        // Then - must not throw NullPointerException; uid should resolve to null
        assertThat(result).isNotNull();
        assertThat(result.uid()).isNull();
    }

    @Test
    void toTrustOnboardingTaskDtoTest_entityNamePassthrough() {
        // Given - the CBS localized map is passed through verbatim, including its "default" key
        var allowedActions = Set.of(TrustOnboardingTaskActionDto.APPROVE);
        var entityName = Map.of("default", "test name de", "de-CH", "test name de");
        var submission = trustOnboardingSubmission(entityName, LanguageDto.FR);
        var task = trustOnboardingTask();

        // When
        var result = toTrustOnboardingTaskDto(allowedActions, task, submission);

        // Then
        assertThat(result.entityName()).containsEntry("default", "test name de");
    }

    @Test
    void toTrustOnboardingTaskDto_mapsContactPersonOnly_whenSignatoriesNull() {
        var allowedActions = Set.of(TrustOnboardingTaskActionDto.APPROVE);
        var submission = trustOnboardingSubmission();
        submission.setSignatories(null);
        var task = trustOnboardingTask();

        var result = toTrustOnboardingTaskDto(allowedActions, task, submission);

        assertThat(result.contacts())
            .singleElement()
            .satisfies(contact -> {
                assertThat(contact.name()).isEqualTo("John Doe");
                assertThat(contact.type()).isEqualTo(TrustOnboardingTaskContactTypeDto.CONTACT_PERSON);
                assertThat(contact.phone()).isEqualTo("12345");
                assertThat(contact.email()).isEqualTo("test@test.ch");
            });
    }

    @Test
    void toTrustOnboardingTaskDto_appendsSignatoriesAfterContactPerson() {
        var allowedActions = Set.of(TrustOnboardingTaskActionDto.APPROVE);
        var submission = trustOnboardingSubmission();
        var signatoryOne = new SignatoryDto();
        signatoryOne.setFirstName("Alice");
        signatoryOne.setLastName("Signer");
        signatoryOne.setPhone("+41 11 111 11 11");
        signatoryOne.setEmail("alice.signer@example.com");
        var signatoryTwo = new SignatoryDto();
        signatoryTwo.setFirstName("Bruno");
        signatoryTwo.setLastName("CoSign");
        signatoryTwo.setPhone("+41 22 222 22 22");
        signatoryTwo.setEmail("bruno.cosign@example.com");
        submission.setSignatories(List.of(signatoryOne, signatoryTwo));
        var task = trustOnboardingTask();

        var result = toTrustOnboardingTaskDto(allowedActions, task, submission);

        assertThat(result.contacts()).hasSize(3);
        assertThat(result.contacts().get(0).type()).isEqualTo(TrustOnboardingTaskContactTypeDto.CONTACT_PERSON);
        assertThat(result.contacts().get(0).name()).isEqualTo("John Doe");
        assertThat(result.contacts().get(1)).satisfies(c -> {
            assertThat(c.name()).isEqualTo("Alice Signer");
            assertThat(c.type()).isEqualTo(TrustOnboardingTaskContactTypeDto.AUTHORISED_SIGNATORY);
            assertThat(c.phone()).isEqualTo("+41 11 111 11 11");
            assertThat(c.email()).isEqualTo("alice.signer@example.com");
        });
        assertThat(result.contacts().get(2)).satisfies(c -> {
            assertThat(c.name()).isEqualTo("Bruno CoSign");
            assertThat(c.type()).isEqualTo(TrustOnboardingTaskContactTypeDto.AUTHORISED_SIGNATORY);
            assertThat(c.phone()).isEqualTo("+41 22 222 22 22");
            assertThat(c.email()).isEqualTo("bruno.cosign@example.com");
        });
    }

    @Test
    void toTrustOnboardingTaskDto_skipsNullSignatoryEntries() {
        var allowedActions = Set.of(TrustOnboardingTaskActionDto.APPROVE);
        var submission = trustOnboardingSubmission();
        var signatory = new SignatoryDto();
        signatory.setFirstName("Bob");
        signatory.setLastName("Valid");
        signatory.setPhone("99");
        signatory.setEmail("bob@example.com");
        var signatories = new ArrayList<SignatoryDto>();
        signatories.add(null);
        signatories.add(signatory);
        submission.setSignatories(signatories);
        var task = trustOnboardingTask();

        var result = toTrustOnboardingTaskDto(allowedActions, task, submission);

        assertThat(result.contacts()).hasSize(2);
        assertThat(result.contacts().get(1).name()).isEqualTo("Bob Valid");
        assertThat(result.contacts().get(1).type()).isEqualTo(TrustOnboardingTaskContactTypeDto.AUTHORISED_SIGNATORY);
    }
}
