package ch.admin.bj.swiyu.trust.management.modules.management.service;

import static ch.admin.bj.swiyu.trust.management.modules.management.domain.domainevent.DomainEventType.*;
import static java.time.Duration.ofSeconds;
import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

import ch.admin.bj.swiyu.messagetype.ti.BusinessPartnerIdentityStatus;
import ch.admin.bj.swiyu.messagetype.ti.TiBusinessPartnerIdentityActivatedEvent;
import ch.admin.bj.swiyu.messagetype.ti.TiBusinessPartnerIdentityDeactivatedEvent;
import ch.admin.bj.swiyu.messagetype.ti.TiBusinessPartnerIdentityUpdatedEvent;
import ch.admin.bj.swiyu.trust.client.core.business.internal.api.TrustOnboardingSubmissionApi;
import ch.admin.bj.swiyu.trust.client.issuer.management.api.CredentialApi;
import ch.admin.bj.swiyu.trust.client.issuer.management.model.CredentialStatusTypeDto;
import ch.admin.bj.swiyu.trust.client.issuer.oid4vci.api.IssuerOid4VciApi;
import ch.admin.bj.swiyu.trust.management.modules.common.audit.AuditPublisher;
import ch.admin.bj.swiyu.trust.management.modules.management.api.*;
import ch.admin.bj.swiyu.trust.management.modules.management.config.DefaultIdentityProperties;
import ch.admin.bj.swiyu.trust.management.modules.management.config.TrustOnboardingTaskProperties;
import ch.admin.bj.swiyu.trust.management.modules.management.config.issuer.IssuerJwtProperties;
import ch.admin.bj.swiyu.trust.management.modules.management.domain.*;
import ch.admin.bj.swiyu.trust.management.modules.management.domain.TrustStatementPartnerLinkStatus;
import ch.admin.bj.swiyu.trust.management.modules.management.domain.corebusiness.IssuerTrustRootProperties;
import ch.admin.bj.swiyu.trust.management.modules.management.domain.details.TrustStatementPartnerLinkType;
import ch.admin.bj.swiyu.trust.management.modules.management.domain.issuer.IssuerClient;
import ch.admin.bj.swiyu.trust.management.modules.management.domain.issuer.IssuerProperties;
import ch.admin.bj.swiyu.trust.management.modules.management.domain.issuer.MockIssuerClient;
import ch.admin.bj.swiyu.trust.management.modules.management.domain.publisher.OutboxEventPublisher;
import ch.admin.bj.swiyu.trust.management.modules.registry.domain.Statement;
import ch.admin.bj.swiyu.trust.management.modules.registry.service.JsonJwtDeserializer;
import ch.admin.bj.swiyu.trust.management.modules.registry.service.TrustRegistryService;
import ch.admin.bj.swiyu.trust.management.test.*;
import java.time.Instant;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase;
import org.springframework.context.annotation.Import;
import org.springframework.data.domain.PageRequest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.testcontainers.junit.jupiter.Testcontainers;

@ActiveProfiles("test")
@DataJpaTest
@Testcontainers
@ContextConfiguration(initializers = PostgreSQLContainerInitializer.class)
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Import(
    {
        TrustStatementService.class,
        TrustStatementPartnerLinkValidator.class,
        TrustRegistryService.class,
        TrustOnboardingTaskService.class,
        TrustOnboardingTaskDomainService.class,
        StatusListServiceTestConfiguration.class,
        OutboxEventPublisher.class,
        MockAuditPublisherTestConfiguration.class,
        JwtStatementDomainService.class,
        JsonJwtDeserializer.class,
        DomainEventService.class,
        DataJpaTestKafkaConfiguration.class,
        DataJpaTestConfiguration.class,
        BusinessPartnerIdentityService.class,
        AsyncTestConfig.class,
        MockIssuerClient.class,
    }
)
@EnableConfigurationProperties(
    {
        IssuerJwtProperties.class,
        IssuerProperties.class,
        IssuerTrustRootProperties.class,
        TrustOnboardingTaskProperties.class,
        DefaultIdentityProperties.class,
    }
)
class BusinessPartnerIdentityServiceIT {

    @MockitoBean
    IssuerClient issuerClient;

    @MockitoBean
    private TrustOnboardingSubmissionApi trustOnboardingSubmissionApi;

    @MockitoBean
    private CredentialApi credentialApi;

    @MockitoBean
    private IssuerOid4VciApi issuerOid4VciApi;

    @Autowired
    private BusinessPartnerIdentityService businessPartnerIdentityService;

    @Autowired
    private TestRepositories repos;

    @MockitoBean
    private OutboxEventPublisher outboxEventPublisher;

    @Autowired
    private AuditPublisher auditPublisher;

    @Autowired
    private AsyncTestConfig asyncTestConfig;

    @BeforeEach
    void setUp() {
        asyncTestConfig.waitForAsyncOperationsFinished();
        reset(outboxEventPublisher, auditPublisher);
        repos.businessPartnerIdentityRepository.deleteAllInBatch();
        repos.protectedVerificationRepository.deleteAllInBatch();
        repos.trustStatementPartnerLink.deleteAllInBatch();
        repos.domainEventLog.deleteAllInBatch();
        repos.statementRepository.deleteAllInBatch();

        // Default stubs for the external issuer service
        when(issuerClient.getStatusListUri()).thenReturn("https://issuer.example.com/status-list");
        when(issuerClient.issueTrustStatement(any())).thenReturn(
            new IssuerClient.TrustStatementIssuanceResult(UUID.randomUUID(), IssuerTestData.sdjwt())
        );
        when(issuerClient.getCredentialStatus(any())).thenReturn(CredentialStatusTypeDto.ISSUED);
    }

    @Test
    void activate_persistsAndEmitsEvent() {
        var bpi = repos.businessPartnerIdentityRepository.save(
            BusinessPartnerIdentityTestData.newDefaultBusinessPartnerIdentity(BusinessPartnerIdentityStatus.DEACTIVATED)
        );
        repos.commit();
        businessPartnerIdentityService.activate(bpi.getId());
        var persisted = repos.businessPartnerIdentityRepository.findById(bpi.getId()).orElseThrow();
        assertThat(persisted.getStatus()).isEqualTo(BusinessPartnerIdentityStatus.ACTIVE);
        var captor = ArgumentCaptor.forClass(TiBusinessPartnerIdentityActivatedEvent.class);
        verify(outboxEventPublisher).publishBusinessPartnerIdentityActivatedEvent(captor.capture());
        assertThat(captor.getValue().getPayload().getBusinessPartnerIdentityId()).isEqualTo(bpi.getId());
    }

    @Test
    void deactivate_persistsAndEmitsEvent() {
        var bpi = repos.businessPartnerIdentityRepository.save(
            BusinessPartnerIdentityTestData.newDefaultBusinessPartnerIdentity(BusinessPartnerIdentityStatus.ACTIVE)
        );
        repos.commit();
        businessPartnerIdentityService.deactivate(bpi.getId());
        var persisted = repos.businessPartnerIdentityRepository.findById(bpi.getId()).orElseThrow();
        assertThat(persisted.getStatus()).isEqualTo(BusinessPartnerIdentityStatus.DEACTIVATED);
        var captor = ArgumentCaptor.forClass(TiBusinessPartnerIdentityDeactivatedEvent.class);
        verify(outboxEventPublisher).publishBusinessPartnerIdentityDeactivatedEvent(captor.capture());
        assertThat(captor.getValue().getPayload().getBusinessPartnerIdentityId()).isEqualTo(bpi.getId());
    }

    @Test
    void issueTrustStatements_createsStatementsAndUpdatesEntity() {
        var bpi = repos.businessPartnerIdentityRepository.save(
            BusinessPartnerIdentityTestData.newDefaultBusinessPartnerIdentity(BusinessPartnerIdentityStatus.ACTIVE)
        );
        // add a PVA
        var pva = new ProtectedVerificationAuthorization(
            UUID.randomUUID(),
            bpi.getId(),
            ProtectedVerificationField.AHV_NUMBER
        );
        repos.protectedVerificationRepository.save(pva);
        repos.commit();
        businessPartnerIdentityService.issueTrustStatements(bpi.getId());
        // three partner links should exist (V1, V2, PVA V2)
        var links = repos.trustStatementPartnerLink.findAll();

        assertThat(links)
            .hasSize(3)
            .allMatch(l -> l.getStatus() == TrustStatementPartnerLinkStatus.ACTIVE)
            .extracting(TrustStatementPartnerLink::getType)
            .containsExactlyInAnyOrder(
                TrustStatementPartnerLinkType.TRUST_STATEMENT_IDENTITY_V1,
                TrustStatementPartnerLinkType.TRUST_STATEMENT_IDENTITY_V2,
                TrustStatementPartnerLinkType.TRUST_STATEMENT_PROTECTED_VERIFICATION_AUTHORIZATION_V2
            );
        // registry statements stored
        var statements = repos.statementRepository.findAll();
        assertThat(statements)
            .hasSize(2) // TRUST_STATEMENT_IDENTITY_V1 is not found in statements repository
            .extracting(Statement::getType)
            .containsExactlyInAnyOrder(
                ch.admin.bj.swiyu.trust.management.modules.registry.domain.StatementType.IDENTITY_TRUST_STATEMENT_V2,
                ch.admin.bj.swiyu.trust.management.modules.registry.domain.StatementType.PROTECTED_VERIFICATION_AUTHORIZATION_TRUST_STATEMENT_V2
            );
        var refreshed = repos.businessPartnerIdentityRepository.findById(bpi.getId()).orElseThrow();
        assertThat(refreshed.getLastIssuanceAt()).isCloseTo(Instant.now(), within(ofSeconds(2)));
    }

    @Test
    void addProtectedVerificationAuthorization_persistsAndEmitsEvents() {
        var bpi = repos.businessPartnerIdentityRepository.save(
            BusinessPartnerIdentityTestData.newDefaultBusinessPartnerIdentity(BusinessPartnerIdentityStatus.ACTIVE)
        );
        repos.commit();
        var request = new ProtectedVerificationAuthorizationRequestDto(bpi.getId(), AuthorizableFieldDto.AHV_NUMBER);
        var dto = businessPartnerIdentityService.addProtectedVerificationAuthorization(request);
        // persistence
        var pvas = repos.protectedVerificationRepository.findAllByBusinessPartnerIdentityId(bpi.getId());
        assertThat(pvas).hasSize(1);
        assertThat(pvas.getFirst().getProtectedVerificationField()).isEqualTo(ProtectedVerificationField.AHV_NUMBER);
        // domain event logged
        var events = repos.domainEventLog.findAll();
        assertThat(events).hasSize(1);
        assertThat(events.getFirst().getEventType()).isEqualTo(PROTECTED_VERIFICATION_AUTHORIZATION_ADDED);
        // BPI update event emitted
        var captor = ArgumentCaptor.forClass(TiBusinessPartnerIdentityUpdatedEvent.class);
        verify(outboxEventPublisher).publishBusinessPartnerIdentityUpdatedEvent(captor.capture());
        assertThat(captor.getValue().getPayload().getBusinessPartnerIdentityId()).isEqualTo(bpi.getId());
        // dto matches persisted entity
        assertThat(dto.id()).isEqualTo(pvas.getFirst().getId());
    }

    @Test
    void removeProtectedVerificationAuthorization_deletesAndEmitsUpdateEvent() {
        var bpi = repos.businessPartnerIdentityRepository.save(
            BusinessPartnerIdentityTestData.newDefaultBusinessPartnerIdentity(BusinessPartnerIdentityStatus.ACTIVE)
        );
        var pva = repos.protectedVerificationRepository.save(
            new ProtectedVerificationAuthorization(
                UUID.randomUUID(),
                bpi.getId(),
                ProtectedVerificationField.AHV_NUMBER
            )
        );
        repos.commit();
        businessPartnerIdentityService.removeProtectedVerificationAuthorization(pva.getId());
        var remaining = repos.protectedVerificationRepository.findAllByBusinessPartnerIdentityId(bpi.getId());
        assertThat(remaining).isEmpty();
        var captor = ArgumentCaptor.forClass(TiBusinessPartnerIdentityUpdatedEvent.class);
        verify(outboxEventPublisher).publishBusinessPartnerIdentityUpdatedEvent(captor.capture());
        assertThat(captor.getValue().getPayload().getBusinessPartnerIdentityId()).isEqualTo(bpi.getId());
    }

    @Test
    void sync_emitsUpdateEvent() {
        var bpi = repos.businessPartnerIdentityRepository.save(
            BusinessPartnerIdentityTestData.newDefaultBusinessPartnerIdentity(BusinessPartnerIdentityStatus.ACTIVE)
        );
        repos.commit();
        businessPartnerIdentityService.sync(bpi.getId());
        var captor = ArgumentCaptor.forClass(TiBusinessPartnerIdentityUpdatedEvent.class);
        verify(outboxEventPublisher).publishBusinessPartnerIdentityUpdatedEvent(captor.capture());
        assertThat(captor.getValue().getPayload().getBusinessPartnerIdentityId()).isEqualTo(bpi.getId());
    }

    @Test
    void syncAll_emitsUpdateEvent() {
        var bpi1 = repos.businessPartnerIdentityRepository.save(
            BusinessPartnerIdentityTestData.newDefaultBusinessPartnerIdentity(BusinessPartnerIdentityStatus.ACTIVE)
        );
        var bpi2 = repos.businessPartnerIdentityRepository.save(
            BusinessPartnerIdentityTestData.newDefaultBusinessPartnerIdentity(BusinessPartnerIdentityStatus.ACTIVE)
        );
        repos.commit();

        businessPartnerIdentityService.syncAll();

        var captor = ArgumentCaptor.forClass(TiBusinessPartnerIdentityUpdatedEvent.class);
        verify(outboxEventPublisher, times(2)).publishBusinessPartnerIdentityUpdatedEvent(captor.capture());
        assertThat(captor.getAllValues())
            .extracting(event -> event.getPayload().getBusinessPartnerIdentityId())
            .containsExactlyInAnyOrder(bpi1.getId(), bpi2.getId());
    }

    @Test
    void getBusinessPartnerIdentities_filtersByCreatedBy() {
        repos.businessPartnerIdentityRepository.save(
            BusinessPartnerIdentityTestData.newDefaultBusinessPartnerIdentity(BusinessPartnerIdentityStatus.ACTIVE)
        );
        repos.businessPartnerIdentityRepository.save(
            BusinessPartnerIdentityTestData.newDefaultBusinessPartnerIdentity(BusinessPartnerIdentityStatus.ACTIVE)
        );
        repos.commit();
        var filter = new BusinessPartnerIdentityFilterDto(null, null, "TestUser");
        var page = businessPartnerIdentityService.getBusinessPartnerIdentities(filter, PageRequest.of(0, 10));
        assertThat(page.getTotalElements()).isEqualTo(2);
    }
}
