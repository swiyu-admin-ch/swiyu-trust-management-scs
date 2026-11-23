package ch.admin.bj.swiyu.trust.management.modules.jobs.service;

import static ch.admin.bj.swiyu.trust.management.modules.common.security.SecurityContextSupport.SYSTEM_USER;
import static ch.admin.bj.swiyu.trust.management.test.TaskTestData.trustOnboardingTask;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

import ch.admin.bj.swiyu.trust.management.modules.management.domain.task.DeleteBusinessPartnerTask;
import ch.admin.bj.swiyu.trust.management.modules.management.service.DeleteBusinessPartnerTaskService;
import ch.admin.bj.swiyu.trust.management.modules.management.service.DomainEventService;
import ch.admin.bj.swiyu.trust.management.modules.management.service.TaskService;
import ch.admin.bj.swiyu.trust.management.test.DataJpaTestConfiguration;
import ch.admin.bj.swiyu.trust.management.test.PostgreSQLContainerInitializer;
import ch.admin.bj.swiyu.trust.management.test.TestRepositories;
import java.time.Instant;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.testcontainers.junit.jupiter.Testcontainers;

/**
 * The job itself only selects and loops; approving a single task is {@link DeleteBusinessPartnerTaskService}'s job and
 * tested there. So the service is mocked here and the assertions are about which tasks the filter lets through and that
 * one failure does not stop the loop.
 */
@Testcontainers
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@ContextConfiguration(initializers = PostgreSQLContainerInitializer.class)
@DataJpaTest
@Import(
    { DataJpaTestConfiguration.class, TaskService.class, DomainEventService.class, HardDeleteBusinessPartnerJob.class }
)
@ActiveProfiles("test")
class HardDeleteBusinessPartnerJobIT {

    @Autowired
    private HardDeleteBusinessPartnerJob job;

    @Autowired
    private TestRepositories repos;

    @MockitoBean
    private DeleteBusinessPartnerTaskService deleteBusinessPartnerTaskService;

    @BeforeEach
    void setUp() {
        repos.domainEventLog.deleteAllInBatch();
        repos.task.deleteAllInBatch();
    }

    @Test
    void approveDeleteTasksThatAreDue_approvesOnlyTasksWhoseWaitingPeriodIsOver() {
        var dueTask = repos.task.saveAndFlush(deleteTask(Instant.now().minusSeconds(60)));
        var notYetDueTask = repos.task.saveAndFlush(deleteTask(Instant.now().plusSeconds(3600)));
        var onboardingTask = repos.task.saveAndFlush(trustOnboardingTask());

        job.approveDeleteTasksThatAreDue();

        verify(deleteBusinessPartnerTaskService).approve(dueTask.getId(), SYSTEM_USER);
        verify(deleteBusinessPartnerTaskService, never()).approve(eq(notYetDueTask.getId()), any());
        verify(deleteBusinessPartnerTaskService, never()).approve(eq(onboardingTask.getId()), any());
    }

    @Test
    void approveDeleteTasksThatAreDue_whenOneTaskFails_thenTheOthersAreStillApproved() {
        var failingTask = repos.task.saveAndFlush(deleteTask(Instant.now().minusSeconds(60)));
        var otherTask = repos.task.saveAndFlush(deleteTask(Instant.now().minusSeconds(30)));
        doThrow(new IllegalStateException("optimistic lock"))
            .when(deleteBusinessPartnerTaskService)
            .approve(failingTask.getId(), SYSTEM_USER);

        job.approveDeleteTasksThatAreDue();

        verify(deleteBusinessPartnerTaskService).approve(otherTask.getId(), SYSTEM_USER);
    }

    private static DeleteBusinessPartnerTask deleteTask(Instant hardDeleteDueAt) {
        return new DeleteBusinessPartnerTask(UUID.randomUUID(), "JobTest Partner", Instant.now(), hardDeleteDueAt);
    }
}
