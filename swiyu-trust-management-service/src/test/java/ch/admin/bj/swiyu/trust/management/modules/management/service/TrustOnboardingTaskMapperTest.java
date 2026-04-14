package ch.admin.bj.swiyu.trust.management.modules.management.service;

import static ch.admin.bj.swiyu.trust.management.modules.management.service.TrustOnboardingTaskMapper.toTrustOnboardingTaskDto;
import static ch.admin.bj.swiyu.trust.management.modules.management.service.TrustOnboardingTaskMapper.toTrustOnboardingTaskListItemDto;
import static ch.admin.bj.swiyu.trust.management.test.TrustOnboardingSubmissionTestData.trustOnboardingSubmission;
import static ch.admin.bj.swiyu.trust.management.test.TrustOnboardingTestData.trustOnboardingTask;
import static org.assertj.core.api.Assertions.assertThat;

import ch.admin.bj.swiyu.trust.management.modules.management.api.TrustOnboardingTaskActionDto;
import ch.admin.bj.swiyu.trust.management.modules.ui.api.BusinessPartnerTypeDto;
import ch.admin.bj.swiyu.trust.management.modules.ui.api.TrustOnboardingTaskDto;
import java.util.HashMap;
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
        assertThat(result.entityName().get(TrustOnboardingTaskDto.LanguageDto.DE_CH)).isEqualTo(
            submission.getEntityName().getDe()
        );
        assertThat(result.entityNameDefault()).isEqualTo("EntityDE");
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
}
