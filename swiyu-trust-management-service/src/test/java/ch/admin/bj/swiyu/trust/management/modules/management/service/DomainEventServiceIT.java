package ch.admin.bj.swiyu.trust.management.modules.management.service;

import static ch.admin.bj.swiyu.trust.management.modules.ui.api.DomainEventTypeDto.*;
import static ch.admin.bj.swiyu.trust.management.test.TrustOnboardingTestData.trustAddDidTask;
import static ch.admin.bj.swiyu.trust.management.test.TrustOnboardingTestData.trustOnboardingTask;
import static org.assertj.core.api.AssertionsForClassTypes.assertThat;

import ch.admin.bj.swiyu.trust.management.modules.management.domain.TrustAddDidTask;
import ch.admin.bj.swiyu.trust.management.modules.management.domain.TrustAddDidTaskRepository;
import ch.admin.bj.swiyu.trust.management.modules.management.domain.TrustOnboardingTask;
import ch.admin.bj.swiyu.trust.management.modules.management.domain.TrustOnboardingTaskRepository;
import ch.admin.bj.swiyu.trust.management.modules.management.domain.domainevent.DomainEventLogRepository;
import ch.admin.bj.swiyu.trust.management.test.DataJpaTestConfiguration;
import ch.admin.bj.swiyu.trust.management.test.PostgreSQLContainerInitializer;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.context.annotation.Import;
import org.springframework.data.domain.Pageable;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.ContextConfiguration;
import org.testcontainers.junit.jupiter.Testcontainers;

@Testcontainers
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@ContextConfiguration(initializers = PostgreSQLContainerInitializer.class)
@DataJpaTest
@Import({ DataJpaTestConfiguration.class, DomainEventService.class })
@ActiveProfiles("test")
class DomainEventServiceIT {

    @Autowired
    private DomainEventLogRepository domainEventLogRepository;

    @Autowired
    private TrustOnboardingTaskRepository trustOnboardingTaskRepository;

    @Autowired
    private TrustAddDidTaskRepository trustAddDidTaskRepository;

    @Autowired
    private DomainEventService domainEventService;

    private TrustOnboardingTask task;
    private TrustAddDidTask addDidTask;

    @BeforeEach
    void setUp() {
        domainEventLogRepository.deleteAllInBatch();
        trustAddDidTaskRepository.deleteAllInBatch();
        trustOnboardingTaskRepository.deleteAllInBatch();
        this.task = trustOnboardingTaskRepository.save(trustOnboardingTask());
        this.addDidTask = trustAddDidTaskRepository.save(trustAddDidTask());
    }

    @Test
    void trustOnboardingSubmissionReceived() {
        // when
        domainEventService.trustOnboardingSubmissionReceived(task.getId(), "test-user");
        // then
        var events = domainEventService.getDomainEventLogs(task.getId(), Pageable.unpaged());
        assertThat(events.getTotalElements()).isEqualTo(1);
        assertThat(events.getContent().getFirst().eventType()).isEqualTo(TRUST_ONBOARDING_SUBMISSION_RECEIVED);
        assertThat(events.getContent().getFirst().triggeredBy()).isEqualTo("test-user");
    }

    @Test
    void trustOnboardingSubmissionSucceeded() {
        // when
        domainEventService.trustOnboardingSubmissionSucceeded(
            task.getId(),
            "test-user",
            "partner note",
            "internal note"
        );
        // then
        var events = domainEventService.getDomainEventLogs(task.getId(), Pageable.unpaged());
        assertThat(events.getTotalElements()).isEqualTo(1);
        assertThat(events.getContent().getFirst().eventType()).isEqualTo(TRUST_ONBOARDING_SUCCEEDED);
        assertThat(events.getContent().getFirst().triggeredBy()).isEqualTo("test-user");
        assertThat(events.getContent().getFirst().partnerNote()).isEqualTo("partner note");
        assertThat(events.getContent().getFirst().internalNote()).isEqualTo("internal note");
    }

    @Test
    void trustOnboardingSubmissionRejected() {
        // when
        domainEventService.trustOnboardingSubmissionRejected(
            task.getId(),
            "test-user",
            "partner note",
            "internal note"
        );
        // then
        var events = domainEventService.getDomainEventLogs(task.getId(), Pageable.unpaged());
        assertThat(events.getTotalElements()).isEqualTo(1);
        assertThat(events.getContent().getFirst().eventType()).isEqualTo(TRUST_ONBOARDING_REJECTED);
        assertThat(events.getContent().getFirst().triggeredBy()).isEqualTo("test-user");
    }

    @Test
    void trustOnboardingSubmissionMoreInformationRequested() {
        // when
        domainEventService.trustOnboardingSubmissionMoreInformationRequested(
            task.getId(),
            "test-user",
            "partner note",
            "internal note"
        );
        // then
        var events = domainEventService.getDomainEventLogs(task.getId(), Pageable.unpaged());
        assertThat(events.getTotalElements()).isEqualTo(1);
        assertThat(events.getContent().getFirst().eventType()).isEqualTo(TRUST_ONBOARDING_MORE_INFORMATION_REQUESTED);
        assertThat(events.getContent().getFirst().triggeredBy()).isEqualTo("test-user");
        assertThat(events.getContent().getFirst().partnerNote()).isEqualTo("partner note");
    }

    @Test
    void trustOnboardingSubmissionTaskNoteAdded() {
        // when
        domainEventService.trustOnboardingSubmissionTaskNoteAdded(task.getId(), "test-user", "internal note");
        // then
        var events = domainEventService.getDomainEventLogs(task.getId(), Pageable.unpaged());
        assertThat(events.getTotalElements()).isEqualTo(1);
        assertThat(events.getContent().getFirst().eventType()).isEqualTo(TRUST_ONBOARDING_TASK_NOTE_ADDED);
        assertThat(events.getContent().getFirst().triggeredBy()).isEqualTo("test-user");
        assertThat(events.getContent().getFirst().internalNote()).isEqualTo("internal note");
    }

    @Test
    void trustAddDidSubmissionReceived() {
        // when
        domainEventService.trustAddDidSubmissionReceived(addDidTask.getId(), "test-user");
        // then
        var events = domainEventService.getDomainEventLogs(addDidTask.getId(), Pageable.unpaged());
        assertThat(events.getTotalElements()).isEqualTo(1);
        assertThat(events.getContent().getFirst().eventType()).isEqualTo(TRUST_ADD_DID_SUBMISSION_RECEIVED);
        assertThat(events.getContent().getFirst().triggeredBy()).isEqualTo("test-user");
    }

    @Test
    void trustAddDidSubmissionSucceeded() {
        // when
        domainEventService.trustAddDidSubmissionSucceeded(addDidTask.getId(), "test-user");
        // then
        var events = domainEventService.getDomainEventLogs(addDidTask.getId(), Pageable.unpaged());
        assertThat(events.getTotalElements()).isEqualTo(1);
        assertThat(events.getContent().getFirst().eventType()).isEqualTo(TRUST_ADD_DID_SUCCEEDED);
        assertThat(events.getContent().getFirst().triggeredBy()).isEqualTo("test-user");
    }

    @Test
    void trustAddDidSubmissionRejected() {
        // when
        domainEventService.trustAddDidSubmissionRejected(addDidTask.getId(), "test-user", "reject reason");
        // then
        var events = domainEventService.getDomainEventLogs(addDidTask.getId(), Pageable.unpaged());
        assertThat(events.getTotalElements()).isEqualTo(1);
        assertThat(events.getContent().getFirst().eventType()).isEqualTo(TRUST_ADD_DID_REJECTED);
        assertThat(events.getContent().getFirst().triggeredBy()).isEqualTo("test-user");
        assertThat(events.getContent().getFirst().internalNote()).isEqualTo("reject reason");
    }
}
