package ch.admin.bj.swiyu.trust.management.modules.management.service;

import static ch.admin.bj.swiyu.trust.management.test.TaskTestData.trustOnboardingTask;
import static ch.admin.bj.swiyu.trust.management.test.TestTransactionSupport.commit;
import static org.assertj.core.api.Assertions.assertThat;

import ch.admin.bj.swiyu.trust.management.modules.management.api.task.TaskFilterDto;
import ch.admin.bj.swiyu.trust.management.test.DataJpaTestConfiguration;
import ch.admin.bj.swiyu.trust.management.test.PostgreSQLContainerInitializer;
import ch.admin.bj.swiyu.trust.management.test.TestRepositories;
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
        var filter = new TaskFilterDto(null, null, null, null, null, null, null);
        // when
        taskService.getTasks(Pageable.unpaged(), filter, "test user");
        // then
        assertThat(repos.task.count()).isEqualTo(2);
    }
}
