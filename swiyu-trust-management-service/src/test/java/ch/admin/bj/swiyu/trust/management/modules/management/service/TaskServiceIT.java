package ch.admin.bj.swiyu.trust.management.modules.management.service;

import static ch.admin.bj.swiyu.trust.management.test.BusinessPartnerIdentityTestData.BUSINESS_PARTNER_NAME;
import static ch.admin.bj.swiyu.trust.management.test.TaskTestData.trustOnboardingTask;
import static ch.admin.bj.swiyu.trust.management.test.TestTransactionSupport.commit;
import static org.assertj.core.api.Assertions.assertThat;

import ch.admin.bj.swiyu.trust.management.modules.management.api.task.TaskFilterDto;
import ch.admin.bj.swiyu.trust.management.modules.management.api.task.TaskListItemDto;
import ch.admin.bj.swiyu.trust.management.modules.management.domain.task.DeleteBusinessPartnerTask;
import ch.admin.bj.swiyu.trust.management.modules.management.domain.task.Task;
import ch.admin.bj.swiyu.trust.management.modules.management.domain.task.TaskStatus;
import ch.admin.bj.swiyu.trust.management.modules.management.domain.task.TaskType;
import ch.admin.bj.swiyu.trust.management.modules.management.domain.task.TrustOnboardingTask;
import ch.admin.bj.swiyu.trust.management.test.DataJpaTestConfiguration;
import ch.admin.bj.swiyu.trust.management.test.PostgreSQLContainerInitializer;
import ch.admin.bj.swiyu.trust.management.test.TestRepositories;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase;
import org.springframework.context.annotation.Import;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.ContextConfiguration;
import org.testcontainers.junit.jupiter.Testcontainers;

@Testcontainers
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@ContextConfiguration(initializers = PostgreSQLContainerInitializer.class)
@DataJpaTest
@Import({ DataJpaTestConfiguration.class, TaskService.class, DomainEventService.class })
@ActiveProfiles("test")
class TaskServiceIT {

    @Autowired
    private TaskService taskService;

    @Autowired
    private TestRepositories repos;

    @BeforeEach
    void setUp() {
        repos.domainEventLog.deleteAllInBatch();
        repos.task.deleteAllInBatch();
    }

    @Test
    void addInternalNote() {
        // given
        var note = "This actor needs some more validation";
        var triggeredBy = "Tina Trusty";
        var taskId = repos.task.save(trustOnboardingTask()).getId();
        commit();

        // when
        taskService.addInternalNote(taskId, note, triggeredBy);

        // then
        var event = repos.domainEventLog.findAll(Sort.by(Sort.Order.desc("triggeredAt"))).getFirst();
        assertThat(event.getInternalNote()).isEqualTo(note);
    }

    @Test
    void assign() {
        // given
        var taskId = repos.task.save(trustOnboardingTask()).getId();
        commit();
        // when
        taskService.assign(taskId, "Timo Truster", "Tina Trusty");

        // then
        var task = repos.task.findById(taskId).orElseThrow();
        assertThat(task.getAssignee()).isEqualTo("Timo Truster");
    }

    @Test
    void getTasks() {
        // given
        repos.task.save(trustOnboardingTask());
        repos.task.save(trustOnboardingTask());
        commit();
        var filter = new TaskFilterDto(null, null, null, null, null, null, null, null);
        // when
        taskService.getTasks(Pageable.unpaged(), filter, "test user");
        // then
        assertThat(repos.task.count()).isEqualTo(2);
    }

    @Test
    void getTasks_whenHardDeleteDueUntilIsSet_thenOnlyDueDeleteTasksAreReturned() {
        var duePartnerId = UUID.randomUUID();
        var dueTask = repos.task.saveAndFlush(deleteTask(duePartnerId, Instant.now().minusSeconds(3600)));
        var notYetDueTask = repos.task.saveAndFlush(deleteTask(UUID.randomUUID(), Instant.now().plusSeconds(3600)));
        var onboardingTask = repos.task.saveAndFlush(trustOnboardingTask());

        var filter = new TaskFilterDto(
            null,
            null,
            null,
            null,
            List.of(TaskStatus.OPENED.name()),
            null,
            List.of(TaskType.DELETE_BUSINESS_PARTNER.name()),
            Instant.now()
        );

        var tasks = taskService.getTasks(Pageable.unpaged(), filter, "test user");

        assertThat(tasks.getContent()).extracting(TaskListItemDto::id).containsExactly(dueTask.getId());
        assertThat(tasks.getContent()).noneMatch(task -> task.id().equals(notYetDueTask.getId()));
        assertThat(tasks.getContent()).noneMatch(task -> task.id().equals(onboardingTask.getId()));
    }

    private static DeleteBusinessPartnerTask deleteTask(UUID partnerId, Instant hardDeleteDueAt) {
        return new DeleteBusinessPartnerTask(partnerId, "FilterTest Partner", Instant.now(), hardDeleteDueAt);
    }

    @Test
    void hardDeleteByPartnerId_deletesAllTasksOfThatPartnerIncludingTheirSubtypeRows() {
        var partnerId = UUID.randomUUID();
        repos.task.saveAndFlush(
            TrustOnboardingTask.createRegistrationTask(
                UUID.randomUUID(),
                partnerId,
                BUSINESS_PARTNER_NAME,
                UUID.randomUUID(),
                Instant.now().plusSeconds(3600),
                Instant.now()
            )
        );
        var otherTask = repos.task.saveAndFlush(
            TrustOnboardingTask.createRegistrationTask(
                UUID.randomUUID(),
                UUID.randomUUID(),
                BUSINESS_PARTNER_NAME,
                UUID.randomUUID(),
                Instant.now().plusSeconds(3600),
                Instant.now()
            )
        );

        taskService.hardDeleteByPartnerId(partnerId);

        assertThat(repos.task.findAll()).extracting(Task::getPartnerId).containsOnly(otherTask.getPartnerId());
        assertThat(repos.trustOnboardingTask.findAll())
            .extracting(TrustOnboardingTask::getId)
            .containsExactly(otherTask.getId());
    }
}
