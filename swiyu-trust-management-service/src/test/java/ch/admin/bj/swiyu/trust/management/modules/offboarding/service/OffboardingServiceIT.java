package ch.admin.bj.swiyu.trust.management.modules.offboarding.service;

import static ch.admin.bj.swiyu.trust.management.modules.management.domain.ProtectedVerificationField.AHV_NUMBER;
import static ch.admin.bj.swiyu.trust.management.modules.management.domain.TrustStatementPartnerLinkStatus.ACTIVE;
import static ch.admin.bj.swiyu.trust.management.modules.management.domain.TrustStatementPartnerLinkStatus.INACTIVE;
import static ch.admin.bj.swiyu.trust.management.modules.management.domain.details.TrustStatementPartnerLinkType.STATEMENT_TOMBSTONE;
import static ch.admin.bj.swiyu.trust.management.test.BusinessPartnerIdentityTestData.BUSINESS_PARTNER_NAME;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import ch.admin.bj.swiyu.trust.client.core.business.internal.api.IdentifierApi;
import ch.admin.bj.swiyu.trust.management.modules.common.audit.AuditPublisher;
import ch.admin.bj.swiyu.trust.management.modules.common.exception.BusinessPartnerDeletionError;
import ch.admin.bj.swiyu.trust.management.modules.common.exception.BusinessPartnerNotDeletableException;
import ch.admin.bj.swiyu.trust.management.modules.management.config.DefaultIdentityProperties;
import ch.admin.bj.swiyu.trust.management.modules.management.config.issuer.IssuerJwtConfig;
import ch.admin.bj.swiyu.trust.management.modules.management.config.issuer.IssuerJwtProperties;
import ch.admin.bj.swiyu.trust.management.modules.management.config.statements.DefaultStatementProperties;
import ch.admin.bj.swiyu.trust.management.modules.management.domain.BusinessPartner;
import ch.admin.bj.swiyu.trust.management.modules.management.domain.BusinessPartnerIdentity;
import ch.admin.bj.swiyu.trust.management.modules.management.domain.BusinessPartnerIdentityDomainService;
import ch.admin.bj.swiyu.trust.management.modules.management.domain.BusinessPartnerIdentityStatus;
import ch.admin.bj.swiyu.trust.management.modules.management.domain.BusinessPartnerType;
import ch.admin.bj.swiyu.trust.management.modules.management.domain.JwtStatementDomainService;
import ch.admin.bj.swiyu.trust.management.modules.management.domain.NonCompliantActor;
import ch.admin.bj.swiyu.trust.management.modules.management.domain.NonCompliantReasonText;
import ch.admin.bj.swiyu.trust.management.modules.management.domain.ProtectedIssuanceAuthorization;
import ch.admin.bj.swiyu.trust.management.modules.management.domain.ProtectedIssuanceEntry;
import ch.admin.bj.swiyu.trust.management.modules.management.domain.ProtectedVerificationAuthorization;
import ch.admin.bj.swiyu.trust.management.modules.management.domain.StatusListDomainService;
import ch.admin.bj.swiyu.trust.management.modules.management.domain.TrustStatementPartnerLink;
import ch.admin.bj.swiyu.trust.management.modules.management.domain.TrustStatementPartnerLinkValidator;
import ch.admin.bj.swiyu.trust.management.modules.management.domain.corebusiness.IssuerTrustRootProperties;
import ch.admin.bj.swiyu.trust.management.modules.management.domain.publisher.OutboxEventPublisher;
import ch.admin.bj.swiyu.trust.management.modules.management.domain.task.Task;
import ch.admin.bj.swiyu.trust.management.modules.management.domain.task.TrustOnboardingTask;
import ch.admin.bj.swiyu.trust.management.modules.management.service.BusinessPartnerIdentityService;
import ch.admin.bj.swiyu.trust.management.modules.management.service.BusinessPartnerService;
import ch.admin.bj.swiyu.trust.management.modules.management.service.DomainEventService;
import ch.admin.bj.swiyu.trust.management.modules.management.service.NonCompliantActorDidsResolver;
import ch.admin.bj.swiyu.trust.management.modules.management.service.NonCompliantActorPublicationService;
import ch.admin.bj.swiyu.trust.management.modules.management.service.NonCompliantActorService;
import ch.admin.bj.swiyu.trust.management.modules.management.service.ProtectedIssuanceService;
import ch.admin.bj.swiyu.trust.management.modules.management.service.TaskService;
import ch.admin.bj.swiyu.trust.management.modules.management.service.TrustStatementService;
import ch.admin.bj.swiyu.trust.management.modules.management.service.VcSchemaAuditService;
import ch.admin.bj.swiyu.trust.management.modules.management.service.VcSchemaPartnerLinkService;
import ch.admin.bj.swiyu.trust.management.modules.registry.domain.DatastoreEntity;
import ch.admin.bj.swiyu.trust.management.modules.registry.domain.VcEntity;
import ch.admin.bj.swiyu.trust.management.modules.registry.domain.VcSchema;
import ch.admin.bj.swiyu.trust.management.modules.registry.service.JsonJwtDeserializer;
import ch.admin.bj.swiyu.trust.management.modules.registry.service.NonComplianceListService;
import ch.admin.bj.swiyu.trust.management.modules.registry.service.TrustRegistryService;
import ch.admin.bj.swiyu.trust.management.modules.registry.service.VcSchemaService;
import ch.admin.bj.swiyu.trust.management.test.AsyncTestConfig;
import ch.admin.bj.swiyu.trust.management.test.DataJpaTestConfiguration;
import ch.admin.bj.swiyu.trust.management.test.DataJpaTestKafkaConfiguration;
import ch.admin.bj.swiyu.trust.management.test.MockAuditPublisherTestConfiguration;
import ch.admin.bj.swiyu.trust.management.test.PostgreSQLContainerInitializer;
import ch.admin.bj.swiyu.trust.management.test.RequestTestData;
import ch.admin.bj.swiyu.trust.management.test.StatusListServiceTestConfiguration;
import ch.admin.bj.swiyu.trust.management.test.TestRepositories;
import java.time.Instant;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.testcontainers.junit.jupiter.Testcontainers;
import tools.jackson.databind.ObjectMapper;

@Testcontainers
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@ContextConfiguration(initializers = PostgreSQLContainerInitializer.class)
@DataJpaTest
@Import(
    {
        OffboardingService.class,
        BusinessPartnerService.class,
        TrustStatementService.class,
        TrustStatementPartnerLinkValidator.class,
        VcSchemaPartnerLinkService.class,
        VcSchemaAuditService.class,
        VcSchemaService.class,
        NonCompliantActorService.class,
        NonCompliantActorDidsResolver.class,
        NonCompliantActorPublicationService.class,
        NonComplianceListService.class,
        ProtectedIssuanceService.class,
        DomainEventService.class,
        TaskService.class,
        BusinessPartnerIdentityDomainService.class,
        BusinessPartnerIdentityService.class,
        JwtStatementDomainService.class,
        StatusListDomainService.class,
        TrustRegistryService.class,
        JsonJwtDeserializer.class,
        OutboxEventPublisher.class,
        IssuerJwtConfig.class,
        DataJpaTestConfiguration.class,
        DataJpaTestKafkaConfiguration.class,
        StatusListServiceTestConfiguration.class,
        MockAuditPublisherTestConfiguration.class,
        AsyncTestConfig.class,
    }
)
@EnableConfigurationProperties(
    {
        IssuerJwtProperties.class,
        DefaultStatementProperties.class,
        DefaultIdentityProperties.class,
        IssuerTrustRootProperties.class,
    }
)
@ActiveProfiles("test")
class OffboardingServiceIT {

    private static final String VC_SCHEMA_FILE =
        "{\"vct\":\"https://trust-reg.trust-infra.swiyu.admin.ch/api/v1/vc-schema/hardDelete\"}";

    @Autowired
    OffboardingService offboardingService;

    @Autowired
    TestRepositories repos;

    @Autowired
    AuditPublisher auditPublisher;

    @Autowired
    AsyncTestConfig asyncTestConfig;

    @Autowired
    DomainEventService domainEventService;

    @Autowired
    TrustStatementService trustStatementService;

    @Autowired
    ObjectMapper objectMapper;

    @MockitoBean
    IdentifierApi identifierApi;

    @BeforeEach
    void setUp() {
        asyncTestConfig.waitForAsyncOperationsFinished();
        reset(auditPublisher);
        repos.vcEntity.deleteAllInBatch();
        repos.datastoreEntity.deleteAllInBatch();
        repos.statement.deleteAllInBatch();
        repos.vcSchema.deleteAllInBatch();
        repos.vcSchemaPartnerLink.deleteAllInBatch();
        repos.protectedIssuanceAuthorization.deleteAllInBatch();
        repos.protectedVerification.deleteAllInBatch();
        repos.protectedIssuanceEntry.deleteAllInBatch();
        repos.domainEventLog.deleteAllInBatch();
        repos.task.deleteAll();
        repos.trustStatementPartnerLink.deleteAllInBatch();
        repos.nonCompliantActor.deleteAllInBatch();
        repos.businessPartnerIdentity.deleteAllInBatch();
        repos.businessPartner.deleteAllInBatch();
    }

    @Test
    void hardDeleteBusinessPartner_removesEverythingOfThatPartnerAndNothingElse() {
        var partnerId = seedPartnerData("deleted");
        var otherPartnerId = seedPartnerData("kept");
        var deletedStatementIds = repos.trustStatementPartnerLink
            .findAllByPartnerId(partnerId)
            .stream()
            .map(TrustStatementPartnerLink::getId)
            .toList();

        offboardingService.hardDeleteBusinessPartner(partnerId);
        asyncTestConfig.waitForAsyncOperationsFinished();

        // statements: tombstoned, public copies gone
        assertThat(repos.trustStatementPartnerLink.findAllByPartnerId(partnerId))
            .hasSize(2)
            .allSatisfy(link -> {
                assertThat(link.getType()).isEqualTo(STATEMENT_TOMBSTONE);
                assertThat(link.getStatus()).isEqualTo(INACTIVE);
                assertThat(link.getSubject()).isNull();
                assertThat(link.getDetails()).isNull();
            });
        assertThat(repos.trustStatementPartnerLink.findAllByPartnerId(otherPartnerId))
            .hasSize(2)
            .allSatisfy(link -> assertThat(link.getType()).isNotEqualTo(STATEMENT_TOMBSTONE));
        assertThat(repos.statement.findAll()).noneMatch(statement -> deletedStatementIds.contains(statement.getId()));
        assertThat(repos.vcSchema.findAll()).hasSize(1);
        assertThat(repos.vcSchemaPartnerLink.findAll())
            .extracting(l -> l.getPartnerId())
            .containsOnly(otherPartnerId);
        // partner rows
        assertThat(repos.nonCompliantActor.findAll())
            .extracting(NonCompliantActor::getBusinessPartnerId)
            .containsOnly(otherPartnerId);
        assertThat(repos.protectedIssuanceAuthorization.findAll())
            .extracting(ProtectedIssuanceAuthorization::getBusinessPartnerIdentityId)
            .containsOnly(otherPartnerId);
        assertThat(repos.protectedVerification.findAll())
            .extracting(ProtectedVerificationAuthorization::getBusinessPartnerIdentityId)
            .containsOnly(otherPartnerId);
        assertThat(repos.task.findAll()).extracting(Task::getPartnerId).containsOnly(otherPartnerId);
        assertThat(repos.businessPartnerIdentity.findById(partnerId)).isEmpty();
        assertThat(repos.businessPartnerIdentity.findById(otherPartnerId)).isPresent();
        assertThat(
            repos.domainEventLog
                .findAll()
                .stream()
                .filter(e -> partnerId.equals(e.getBusinessPartnerId()))
        ).isEmpty();
        // audit
        verify(auditPublisher).vcSchemaDeleted(anyString(), eq(partnerId.toString()), eq(0L), anyString(), anyString());
        verify(auditPublisher).businessPartnerIdentityDeleted(eq(partnerId.toString()), anyLong(), anyString());
        verify(auditPublisher).deactivateTrustStatement(
            anyString(),
            eq(partnerId.toString()),
            eq("TRUST_STATEMENT_IDENTITY_V2"),
            eq(0L),
            anyString(),
            anyString()
        );

        // a second run changes nothing and repeats no side effect
        var statementCount = repos.statement.count();
        var nonComplianceListCount = repos.nonComplianceList.count();
        var partnerLinkCount = repos.trustStatementPartnerLink.count();
        offboardingService.hardDeleteBusinessPartner(partnerId);
        asyncTestConfig.waitForAsyncOperationsFinished();
        assertThat(repos.trustStatementPartnerLink.findAllByPartnerId(partnerId)).hasSize(2);
        assertThat(repos.statement.count()).isEqualTo(statementCount);
        assertThat(repos.nonComplianceList.count()).isEqualTo(nonComplianceListCount);
        assertThat(repos.trustStatementPartnerLink.count()).isEqualTo(partnerLinkCount);
        verify(auditPublisher, times(1)).vcSchemaDeleted(
            anyString(),
            eq(partnerId.toString()),
            eq(0L),
            anyString(),
            anyString()
        );
        verify(auditPublisher, times(1)).businessPartnerIdentityDeleted(
            eq(partnerId.toString()),
            anyLong(),
            anyString()
        );
    }

    @Test
    void hardDeleteBusinessPartner_abortsWhileThePartnerStillExists() {
        var partnerId = seedPartnerData("still-alive");
        var partner = BusinessPartner.createFor(partnerId);
        partner.sync("Acme AG", "CHE-1", BusinessPartnerType.BUSINESS, true, Instant.now());
        repos.businessPartner.saveAndFlush(partner);

        assertThatThrownBy(() -> offboardingService.hardDeleteBusinessPartner(partnerId))
            .isInstanceOf(BusinessPartnerNotDeletableException.class)
            .hasFieldOrPropertyWithValue("error", BusinessPartnerDeletionError.PARTNER_STILL_EXISTS);

        assertThat(repos.businessPartnerIdentity.findById(partnerId)).isPresent();
        assertThat(repos.trustStatementPartnerLink.findAllByPartnerId(partnerId)).allSatisfy(link ->
            assertThat(link.getType()).isNotEqualTo(STATEMENT_TOMBSTONE)
        );
    }

    @Test
    void hardDeleteBusinessPartner_completesForAPartnerWithoutIdentity() {
        var partnerId = UUID.randomUUID();
        repos.nonCompliantActor.saveAndFlush(
            new NonCompliantActor(
                UUID.randomUUID(),
                "did:nca:no-identity",
                partnerId,
                new NonCompliantReasonText(null, null, null, "Reason", null)
            )
        );

        offboardingService.hardDeleteBusinessPartner(partnerId);
        asyncTestConfig.waitForAsyncOperationsFinished();

        assertThat(repos.nonCompliantActor.findAll()).isEmpty();
    }

    private UUID seedPartnerData(String label) {
        var partnerId = UUID.randomUUID();
        var did = "did:example:" + label;
        repos.businessPartnerIdentity.saveAndFlush(
            new BusinessPartnerIdentity(
                partnerId,
                Map.of("default", "Acme " + label),
                Instant.now(),
                "CHE-" + label,
                true,
                "de",
                BusinessPartnerIdentityStatus.ACTIVE,
                false,
                Instant.now().plusSeconds(3600),
                null,
                Set.of(did)
            )
        );
        trustStatementService.issueAndPublishIdentityV2TrustStatement(
            RequestTestData.tsIdentityV2RequestDto(partnerId, did)
        );

        var datastore = repos.datastoreEntity.saveAndFlush(new DatastoreEntity());
        repos.vcEntity.saveAndFlush(new VcEntity(datastore.getId(), "raw-vc", objectMapper.valueToTree(Map.of())));
        var v1 = TrustStatementPartnerLink.createIssuanceV1(
            partnerId,
            did,
            Instant.now(),
            Instant.now().plusSeconds(3600),
            "https://example.com/vct"
        );
        v1.persistReferencesAfterPublicationSucceeded(UUID.randomUUID(), datastore.getId(), ACTIVE);
        repos.trustStatementPartnerLink.saveAndFlush(v1);

        var vcSchema = repos.vcSchema.saveAndFlush(new VcSchema(label, VC_SCHEMA_FILE));
        repos.vcSchemaPartnerLink.saveAndFlush(
            new ch.admin.bj.swiyu.trust.management.modules.management.domain.VcSchemaPartnerLink(
                vcSchema.getId(),
                UUID.randomUUID(),
                partnerId
            )
        );

        repos.nonCompliantActor.saveAndFlush(
            new NonCompliantActor(
                UUID.randomUUID(),
                "did:nca:" + label,
                partnerId,
                new NonCompliantReasonText(null, null, null, "Reason", null)
            )
        );

        var entry = repos.protectedIssuanceEntry.saveAndFlush(
            new ProtectedIssuanceEntry(
                UUID.randomUUID(),
                "https://example.com/vct/" + label,
                Instant.now(),
                Map.of("default", "Example")
            )
        );
        repos.protectedIssuanceAuthorization.saveAndFlush(
            new ProtectedIssuanceAuthorization(UUID.randomUUID(), partnerId, entry.getId(), Map.of("default", "Reason"))
        );
        repos.protectedVerification.saveAndFlush(
            new ProtectedVerificationAuthorization(UUID.randomUUID(), partnerId, AHV_NUMBER)
        );

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
        domainEventService.protectedIssuanceAuthorizationRemoved(UUID.randomUUID(), partnerId, "user");

        return partnerId;
    }
}
