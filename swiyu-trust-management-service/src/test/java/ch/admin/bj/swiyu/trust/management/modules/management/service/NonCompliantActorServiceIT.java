package ch.admin.bj.swiyu.trust.management.modules.management.service;

import static ch.admin.bj.swiyu.trust.management.modules.management.domain.domainevent.DomainEventType.NON_COMPLIANT_ACTOR_ADDED;
import static ch.admin.bj.swiyu.trust.management.modules.management.domain.domainevent.DomainEventType.NON_COMPLIANT_ACTOR_REMOVED;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import ch.admin.bj.swiyu.trust.management.modules.common.exception.ResourceNotFoundException;
import ch.admin.bj.swiyu.trust.management.modules.management.api.NonCompliantActorDto;
import ch.admin.bj.swiyu.trust.management.modules.management.api.NonCompliantActorFilterDto;
import ch.admin.bj.swiyu.trust.management.modules.management.api.NonCompliantActorRequestDto;
import ch.admin.bj.swiyu.trust.management.modules.management.api.NonCompliantReasonTextDto;
import ch.admin.bj.swiyu.trust.management.modules.management.domain.NonCompliantActor;
import ch.admin.bj.swiyu.trust.management.modules.management.domain.NonCompliantActorRepository;
import ch.admin.bj.swiyu.trust.management.modules.management.domain.NonCompliantReasonText;
import ch.admin.bj.swiyu.trust.management.modules.management.domain.domainevent.DomainEventLogRepository;
import ch.admin.bj.swiyu.trust.management.test.DataJpaTestConfiguration;
import ch.admin.bj.swiyu.trust.management.test.PostgreSQLContainerInitializer;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.context.annotation.Import;
import org.springframework.data.domain.PageRequest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.ContextConfiguration;
import org.testcontainers.junit.jupiter.Testcontainers;

@ActiveProfiles("test")
@DataJpaTest
@Testcontainers
@ContextConfiguration(initializers = PostgreSQLContainerInitializer.class)
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Import({ DataJpaTestConfiguration.class, NonCompliantActorService.class, DomainEventService.class })
class NonCompliantActorServiceIT {

    @Autowired
    private NonCompliantActorService service;

    @Autowired
    private NonCompliantActorRepository nonCompliantActorRepository;

    @Autowired
    private DomainEventLogRepository domainEventLogRepository;

    private static final String CURRENT_USER = "Alice Admin";

    @BeforeEach
    void setup() {
        domainEventLogRepository.deleteAllInBatch();
        nonCompliantActorRepository.deleteAllInBatch();
    }

    @Test
    void createNonCompliantActor_success_persists_and_writes_event_log() {
        var did = "did:tdw:alpha123";
        var req = new NonCompliantActorRequestDto(
            did,
            new NonCompliantReasonTextDto(null, null, null, "Violation of policy", null)
        );

        var dto = service.createNonCompliantActor(req, CURRENT_USER);

        assertThat(dto).isNotNull();
        assertThat(dto.did()).isEqualTo(did);
        assertThat(nonCompliantActorRepository.existsNonCompliantActorByDid(did)).isTrue();

        var domainEvent = domainEventLogRepository.findAll().getFirst();
        assertThat(domainEvent.getEventType()).isEqualTo(NON_COMPLIANT_ACTOR_ADDED);
    }

    @Test
    void createNonCompliantActor_fails_when_no_reason_in_any_language() {
        var req = new NonCompliantActorRequestDto(
            "did:tdw:noreason",
            new NonCompliantReasonTextDto(null, " ", null, "", null)
        );

        assertThatThrownBy(() -> service.createNonCompliantActor(req, CURRENT_USER))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("at least one reason");

        assertThat(domainEventLogRepository.count()).isEqualTo(0);
    }

    @Test
    void createNonCompliantActor_fails_on_duplicate_did() {
        // pre-existing entity
        nonCompliantActorRepository.save(
            new NonCompliantActor(
                UUID.randomUUID(),
                "did:tdw:dup",
                new NonCompliantReasonText("Vorfall", null, null, null, null)
            )
        );

        var req = new NonCompliantActorRequestDto(
            "did:tdw:dup",
            new NonCompliantReasonTextDto(null, null, null, "Duplicate", null)
        );

        assertThatThrownBy(() -> service.createNonCompliantActor(req, CURRENT_USER))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("already exists");

        assertThat(domainEventLogRepository.count()).isEqualTo(0);
    }

    @Test
    void getNonCompliantActor_returns_dto_when_found() {
        var saved = nonCompliantActorRepository.save(
            new NonCompliantActor(
                UUID.randomUUID(),
                "did:tdw:get1",
                new NonCompliantReasonText(null, null, null, "X", null)
            )
        );

        var dto = service.getNonCompliantActor(saved.getId());

        assertThat(dto.id()).isEqualTo(saved.getId());
        assertThat(dto.did()).isEqualTo("did:tdw:get1");
    }

    @Test
    void getNonCompliantActor_throws_when_missing() {
        var unknown = UUID.randomUUID();

        assertThatThrownBy(() -> service.getNonCompliantActor(unknown))
            .isInstanceOf(ResourceNotFoundException.class)
            .hasMessageContaining(unknown.toString());
    }

    @Test
    void getNonCompliantActors_filters_by_did_contains() {
        nonCompliantActorRepository.save(
            new NonCompliantActor(
                UUID.randomUUID(),
                "did:tdw:abc-111",
                new NonCompliantReasonText(null, null, null, "A", null)
            )
        );
        nonCompliantActorRepository.save(
            new NonCompliantActor(
                UUID.randomUUID(),
                "did:tdw:def-222",
                new NonCompliantReasonText(null, null, null, "B", null)
            )
        );
        nonCompliantActorRepository.save(
            new NonCompliantActor(
                UUID.randomUUID(),
                "did:tdw:abc-333",
                new NonCompliantReasonText(null, null, null, "C", null)
            )
        );

        var filters = new NonCompliantActorFilterDto("abc");
        var page = service.getNonCompliantActors(filters, PageRequest.of(0, 10));

        assertThat(page.getTotalElements()).isEqualTo(2);
        assertThat(page.getContent())
            .extracting(NonCompliantActorDto::did)
            .allMatch(d -> d.contains("abc"));
    }

    @Test
    void getNonCompliantActors_when_filter_null_returns_all() {
        nonCompliantActorRepository.save(
            new NonCompliantActor(
                UUID.randomUUID(),
                "did:tdw:x1",
                new NonCompliantReasonText(null, null, null, "A", null)
            )
        );
        nonCompliantActorRepository.save(
            new NonCompliantActor(
                UUID.randomUUID(),
                "did:tdw:x2",
                new NonCompliantReasonText(null, null, null, "B", null)
            )
        );

        var page = service.getNonCompliantActors(new NonCompliantActorFilterDto(null), PageRequest.of(0, 10));

        assertThat(page.getTotalElements()).isEqualTo(2);
    }

    @Test
    void deleteNonCompliantActor_deletes_and_writes_event_log() {
        var saved = nonCompliantActorRepository.save(
            new NonCompliantActor(
                UUID.randomUUID(),
                "did:tdw:delme",
                new NonCompliantReasonText(null, null, null, "Reason", null)
            )
        );

        service.deleteNonCompliantActor(saved.getId(), CURRENT_USER);

        assertThat(nonCompliantActorRepository.findById(saved.getId())).isEmpty();
        var domainEvent = domainEventLogRepository.findAll().getFirst();
        assertThat(domainEvent.getEventType()).isEqualTo(NON_COMPLIANT_ACTOR_REMOVED);
    }
}
