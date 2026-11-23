package ch.admin.bj.swiyu.trust.management.modules.management.service;

import static org.assertj.core.api.Assertions.assertThat;

import ch.admin.bj.swiyu.trust.management.modules.management.domain.BusinessPartnerIdentity;
import ch.admin.bj.swiyu.trust.management.modules.management.domain.BusinessPartnerIdentityRepository;
import ch.admin.bj.swiyu.trust.management.modules.management.domain.BusinessPartnerIdentityStatus;
import ch.admin.bj.swiyu.trust.management.modules.management.domain.ProtectedIssuanceAuthorization;
import ch.admin.bj.swiyu.trust.management.modules.management.domain.ProtectedIssuanceAuthorizationRepository;
import ch.admin.bj.swiyu.trust.management.modules.management.domain.ProtectedIssuanceEntry;
import ch.admin.bj.swiyu.trust.management.modules.management.domain.ProtectedIssuanceEntryRepository;
import ch.admin.bj.swiyu.trust.management.modules.management.domain.publisher.OutboxEventPublisher;
import ch.admin.bj.swiyu.trust.management.test.DataJpaTestConfiguration;
import ch.admin.bj.swiyu.trust.management.test.PostgreSQLContainerInitializer;
import java.time.Instant;
import java.util.Map;
import java.util.Set;
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

@Testcontainers
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@ContextConfiguration(initializers = PostgreSQLContainerInitializer.class)
@DataJpaTest
@Import({ ProtectedIssuanceService.class, DataJpaTestConfiguration.class })
@ActiveProfiles("test")
class ProtectedIssuanceServiceIT {

    @Autowired
    ProtectedIssuanceService protectedIssuanceService;

    @Autowired
    ProtectedIssuanceEntryRepository protectedIssuanceEntryRepository;

    @Autowired
    ProtectedIssuanceAuthorizationRepository protectedIssuanceAuthorizationRepository;

    @Autowired
    BusinessPartnerIdentityRepository businessPartnerIdentityRepository;

    @MockitoBean
    TrustStatementService trustStatementService;

    @MockitoBean
    OutboxEventPublisher outboxEventPublisher;

    @MockitoBean
    DomainEventService domainEventService;

    @BeforeEach
    void setUp() {
        protectedIssuanceAuthorizationRepository.deleteAllInBatch();
        protectedIssuanceEntryRepository.deleteAllInBatch();
        businessPartnerIdentityRepository.deleteAllInBatch();
    }

    @Test
    void hardDeleteByPartnerId_deletesOnlyThatPartnersAuthorizationsAndKeepsTheEntries() {
        var entry = protectedIssuanceEntryRepository.saveAndFlush(
            new ProtectedIssuanceEntry(
                UUID.randomUUID(),
                "https://trust-reg.example.ch/api/v1/vc-schema/mySchema",
                Instant.now(),
                Map.of("default", "Example")
            )
        );
        var partnerId = UUID.randomUUID();
        var otherPartnerId = UUID.randomUUID();
        businessPartnerIdentityRepository.saveAndFlush(businessPartnerIdentity(partnerId));
        businessPartnerIdentityRepository.saveAndFlush(businessPartnerIdentity(otherPartnerId));
        protectedIssuanceAuthorizationRepository.saveAndFlush(
            new ProtectedIssuanceAuthorization(UUID.randomUUID(), partnerId, entry.getId(), Map.of("default", "Reason"))
        );
        var otherPartnerAuthorization = protectedIssuanceAuthorizationRepository.saveAndFlush(
            new ProtectedIssuanceAuthorization(
                UUID.randomUUID(),
                otherPartnerId,
                entry.getId(),
                Map.of("default", "Reason")
            )
        );

        protectedIssuanceService.hardDeleteByPartnerId(partnerId);

        assertThat(protectedIssuanceAuthorizationRepository.findAll()).containsExactly(otherPartnerAuthorization);
        assertThat(protectedIssuanceEntryRepository.findById(entry.getId())).isPresent();

        protectedIssuanceService.hardDeleteByPartnerId(partnerId);

        assertThat(protectedIssuanceAuthorizationRepository.findAll()).containsExactly(otherPartnerAuthorization);
    }

    private static BusinessPartnerIdentity businessPartnerIdentity(UUID partnerId) {
        return new BusinessPartnerIdentity(
            partnerId,
            Map.of("default", "Acme AG"),
            Instant.now(),
            "CHE-123.456.789",
            true,
            "de",
            BusinessPartnerIdentityStatus.ACTIVE,
            false,
            Instant.now().plusSeconds(3600),
            null,
            Set.of()
        );
    }
}
