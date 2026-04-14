package ch.admin.bj.swiyu.trust.management.modules.facades;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.any;
import static org.mockito.Mockito.doThrow;

import ch.admin.bj.swiyu.trust.management.modules.management.domain.NonCompliantActor;
import ch.admin.bj.swiyu.trust.management.modules.management.domain.NonCompliantActorRepository;
import ch.admin.bj.swiyu.trust.management.modules.management.domain.NonCompliantReasonText;
import ch.admin.bj.swiyu.trust.management.modules.management.service.NonCompliantActorPublicationService;
import ch.admin.bj.swiyu.trust.management.modules.registry.domain.NonComplianceListRepository;
import ch.admin.bj.swiyu.trust.management.modules.registry.service.NonComplianceListService;
import ch.admin.bj.swiyu.trust.management.test.DataJpaTestConfiguration;
import ch.admin.bj.swiyu.trust.management.test.PostgreSQLContainerInitializer;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.Instant;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.jackson.JacksonAutoConfiguration;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.Primary;
import org.springframework.core.task.SyncTaskExecutor;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.bean.override.mockito.MockitoSpyBean;
import org.testcontainers.junit.jupiter.Testcontainers;

@ActiveProfiles("test")
@DataJpaTest
@Testcontainers
@ContextConfiguration(initializers = PostgreSQLContainerInitializer.class)
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Import(
    {
        DataJpaTestConfiguration.class,
        NonCompliantActorPublicationService.class,
        NonCompliantActorPublicationServiceIT.TestSyncExecutorConfig.class,
        NonComplianceListService.class,
        JacksonAutoConfiguration.class,
    }
)
class NonCompliantActorPublicationServiceIT {

    @Autowired
    private NonCompliantActorPublicationService service;

    @Autowired
    private NonCompliantActorRepository actorRepo;

    @Autowired
    private NonComplianceListRepository listRepo;

    @MockitoSpyBean
    private ObjectMapper objectMapper; // Spied to allow stubbing (forcing serialization failure in one test)

    @BeforeEach
    void setup() {
        listRepo.deleteAllInBatch();
        actorRepo.deleteAllInBatch();
    }

    @Test
    void triggerPublicationAsync_success_persists_snapshot_as_json() {
        actorRepo.save(
            new NonCompliantActor(
                UUID.randomUUID(),
                "did:tdw:pub-1",
                new NonCompliantReasonText(null, null, null, "Reason 1", null)
            )
        );
        actorRepo.save(
            new NonCompliantActor(
                UUID.randomUUID(),
                "did:tdw:pub-2",
                new NonCompliantReasonText("Grund 2", null, null, null, null)
            )
        );

        var before = Instant.now();
        service.triggerPublicationAsync(); // synchronous due to TestSyncExecutorConfig

        assertThat(listRepo.count()).isEqualTo(1);
        var saved = listRepo.findAll().getFirst();

        assertThat(saved.getId()).isNotNull();
        assertThat(saved.getAuditMetadata().getCreatedAt()).isBetween(
            before.minusSeconds(1),
            Instant.now().plusSeconds(1)
        );
        assertThat(saved.getPayload()).contains("did:tdw:pub-1", "did:tdw:pub-2");
    }

    @Test
    void triggerPublicationAsync_when_serialization_fails_does_not_persist() throws Exception {
        actorRepo.save(
            new NonCompliantActor(
                UUID.randomUUID(),
                "did:tdw:failme",
                new NonCompliantReasonText(null, null, null, "X", null)
            )
        );
        doThrow(new JsonProcessingException("boom") {}).when(objectMapper).writeValueAsString(any());
        service.triggerPublicationAsync();
        assertThat(listRepo.count()).isZero();
    }

    /**
     * Enables {@code @Async} support but overrides the executor with a {@link SyncTaskExecutor}
     * so async methods run synchronously in tests for deterministic behavior.
     */
    @TestConfiguration
    @EnableAsync
    static class TestSyncExecutorConfig {

        @Bean(name = { "taskExecutor", "applicationTaskExecutor" })
        @Primary
        SyncTaskExecutor taskExecutor() {
            return new SyncTaskExecutor();
        }
    }
}
