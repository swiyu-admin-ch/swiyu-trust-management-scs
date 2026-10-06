package ch.admin.bj.swiyu.trust.management.modules.management.domain.statistics;

import static ch.admin.bj.swiyu.trust.management.test.BusinessPartnerIdentityTestData.newDefaultBusinessPartnerIdentity;
import static ch.admin.bj.swiyu.trust.management.test.BusinessPartnerIdentityTestData.partnerLinkIdentityV1;
import static ch.admin.bj.swiyu.trust.management.test.TestTransactionSupport.commit;
import static org.assertj.core.api.Assertions.assertThat;

import ch.admin.bj.swiyu.trust.client.core.business.internal.api.IdentifierApi;
import ch.admin.bj.swiyu.trust.client.core.business.internal.api.TrustOnboardingSubmissionApi;
import ch.admin.bj.swiyu.trust.client.issuer.management.api.CredentialApi;
import ch.admin.bj.swiyu.trust.client.issuer.oid4vci.api.IssuerOid4VciApi;
import ch.admin.bj.swiyu.trust.management.modules.jobs.service.TrustStatisticsMetricsExporterJob;
import ch.admin.bj.swiyu.trust.management.modules.management.api.BusinessPartnerIdentityStatusDto;
import ch.admin.bj.swiyu.trust.management.modules.management.api.TrustStatementTypeDto;
import ch.admin.bj.swiyu.trust.management.modules.management.config.DefaultIdentityProperties;
import ch.admin.bj.swiyu.trust.management.modules.management.config.TrustOnboardingTaskProperties;
import ch.admin.bj.swiyu.trust.management.modules.management.config.issuer.IssuerJwtConfig;
import ch.admin.bj.swiyu.trust.management.modules.management.config.issuer.IssuerJwtProperties;
import ch.admin.bj.swiyu.trust.management.modules.management.config.statements.DefaultStatementProperties;
import ch.admin.bj.swiyu.trust.management.modules.management.domain.BusinessPartnerIdentityDomainService;
import ch.admin.bj.swiyu.trust.management.modules.management.domain.BusinessPartnerIdentityStatus;
import ch.admin.bj.swiyu.trust.management.modules.management.domain.JwtStatementDomainService;
import ch.admin.bj.swiyu.trust.management.modules.management.domain.StatusListMetadata;
import ch.admin.bj.swiyu.trust.management.modules.management.domain.TrustStatementPartnerLink;
import ch.admin.bj.swiyu.trust.management.modules.management.domain.TrustStatementPartnerLinkValidator;
import ch.admin.bj.swiyu.trust.management.modules.management.domain.corebusiness.CoreBusinessProperties;
import ch.admin.bj.swiyu.trust.management.modules.management.domain.corebusiness.IssuerTrustRootProperties;
import ch.admin.bj.swiyu.trust.management.modules.management.domain.corebusiness.StatusRegistryProperties;
import ch.admin.bj.swiyu.trust.management.modules.management.domain.issuer.IssuerClient;
import ch.admin.bj.swiyu.trust.management.modules.management.domain.issuer.IssuerProperties;
import ch.admin.bj.swiyu.trust.management.modules.management.domain.issuer.MockIssuerClient;
import ch.admin.bj.swiyu.trust.management.modules.management.domain.publisher.OutboxEventPublisher;
import ch.admin.bj.swiyu.trust.management.modules.management.service.BusinessPartnerIdentityService;
import ch.admin.bj.swiyu.trust.management.modules.management.service.DomainEventService;
import ch.admin.bj.swiyu.trust.management.modules.management.service.NonCompliantActorDidsResolver;
import ch.admin.bj.swiyu.trust.management.modules.management.service.StatusListService;
import ch.admin.bj.swiyu.trust.management.modules.management.service.TrustOnboardingTaskService;
import ch.admin.bj.swiyu.trust.management.modules.management.service.TrustStatementService;
import ch.admin.bj.swiyu.trust.management.modules.registry.service.JsonJwtDeserializer;
import ch.admin.bj.swiyu.trust.management.modules.registry.service.TrustRegistryService;
import ch.admin.bj.swiyu.trust.management.test.*;
import io.micrometer.core.instrument.MeterRegistry;
import java.time.Instant;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.transaction.annotation.Transactional;
import org.testcontainers.junit.jupiter.Testcontainers;

@Testcontainers
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@ContextConfiguration(initializers = PostgreSQLContainerInitializer.class)
@DataJpaTest
@Import(
    {
        IssuerJwtConfig.class,
        JwtStatementDomainService.class,
        JsonJwtDeserializer.class,
        TrustStatementService.class,
        TrustRegistryService.class,
        TrustStatementPartnerLinkValidator.class,
        NonCompliantActorDidsResolver.class,
        BusinessPartnerIdentityService.class,
        BusinessPartnerIdentityDomainService.class,
        DomainEventService.class,
        TrustOnboardingTaskService.class,
        StatusListService.class,
        StatusListServiceTestConfiguration.class,
        MockAuditPublisherTestConfiguration.class,
        DataJpaTestKafkaConfiguration.class,
        MockIssuerClient.class,
        DataJpaTestConfiguration.class,
        MeterRegistryTestConfiguration.class,
        TrustStatisticsMetricsExporterJob.class,
        AsyncTestConfig.class,
    }
)
@EnableConfigurationProperties(
    {
        IssuerJwtProperties.class,
        IssuerProperties.class,
        IssuerTrustRootProperties.class,
        TrustOnboardingTaskProperties.class,
        DefaultIdentityProperties.class,
        DefaultStatementProperties.class,
        CoreBusinessProperties.class,
        StatusRegistryProperties.class,
    }
)
@TestPropertySource(properties = { "app.jobs.statistics.enabled=true" })
@ActiveProfiles("test")
@RequiredArgsConstructor
class TrustStatisticsMetricsExporterJobIT {

    @Autowired
    private TrustStatisticsMetricsExporterJob trustStatisticsMetricsExporterJob;

    @Autowired
    private MeterRegistry meterRegistry;

    @Autowired
    private TestRepositories testRepositories;

    @MockitoBean
    IdentifierApi identifierApi;

    @MockitoBean
    IssuerClient issuerClient;

    @MockitoBean
    private TrustOnboardingSubmissionApi trustOnboardingSubmissionApi;

    @MockitoBean
    private CredentialApi credentialApi;

    @MockitoBean
    private IssuerOid4VciApi issuerOid4VciApi;

    @MockitoBean
    private OutboxEventPublisher outboxEventPublisher;

    @Autowired
    private AsyncTestConfig asyncTestConfig;

    @BeforeEach
    void setUp() {
        asyncTestConfig.waitForAsyncOperationsFinished();
        testRepositories.trustStatementPartnerLink.deleteAllInBatch();
        testRepositories.statusListMetadata.deleteAllInBatch();
        testRepositories.businessPartnerIdentity.deleteAllInBatch();
    }

    @Test
    @Transactional
    void refreshStatistics_updatesCountersFromRepositories() {
        // given
        testRepositories.businessPartnerIdentity.save(
            newDefaultBusinessPartnerIdentity(BusinessPartnerIdentityStatus.ACTIVE)
        );
        testRepositories.businessPartnerIdentity.save(
            newDefaultBusinessPartnerIdentity(BusinessPartnerIdentityStatus.ACTIVE)
        );
        testRepositories.businessPartnerIdentity.save(
            newDefaultBusinessPartnerIdentity(BusinessPartnerIdentityStatus.DEACTIVATED)
        );

        for (var i = 0; i < 7; i++) {
            testRepositories.statusListMetadata.save(
                new StatusListMetadata(UUID.randomUUID(), "https://statusListMetadata/" + i, 100)
            );
        }

        testRepositories.trustStatementPartnerLink.save(partnerLinkIdentityV1("did:identity:a"));
        testRepositories.trustStatementPartnerLink.save(partnerLinkIdentityV1("did:identity:b"));
        testRepositories.trustStatementPartnerLink.save(partnerLinkIdentityV1("did:identity:c"));
        testRepositories.trustStatementPartnerLink.save(
            TrustStatementPartnerLink.createIssuanceV1(
                UUID.randomUUID(),
                "did:issuance:a",
                Instant.now(),
                Instant.parse("2027-01-01T00:00:00Z"),
                "canIssue"
            )
        );
        testRepositories.trustStatementPartnerLink.save(
            TrustStatementPartnerLink.createIssuanceV1(
                UUID.randomUUID(),
                "did:issuance:b",
                Instant.now(),
                Instant.parse("2027-01-01T00:00:00Z"),
                "canIssue"
            )
        );
        commit();
        // when
        trustStatisticsMetricsExporterJob.refreshStatistics();

        // then
        assertThat(meterRegistry.get("swiyu_tms_business_partner_identity").gauge().value()).isEqualTo(3.0);
        assertThat(
            meterRegistry
                .get("swiyu_tms_business_partner_identity_by_status")
                .tag("status", BusinessPartnerIdentityStatusDto.ACTIVE.name())
                .gauge()
                .value()
        ).isEqualTo(2.0);
        assertThat(
            meterRegistry
                .get("swiyu_tms_business_partner_identity_by_status")
                .tag("status", BusinessPartnerIdentityStatusDto.DEACTIVATED.name())
                .gauge()
                .value()
        ).isEqualTo(1.0);
        assertThat(meterRegistry.get("swiyu_tms_status_list").gauge().value()).isEqualTo(7.0);
        assertThat(meterRegistry.get("swiyu_tms_partner_link").gauge().value()).isEqualTo(5.0);
        assertThat(
            meterRegistry
                .get("swiyu_tms_partner_link_by_type")
                .tag("type", TrustStatementTypeDto.IDENTITY_V1.name())
                .gauge()
                .value()
        ).isEqualTo(3.0);
        assertThat(
            meterRegistry
                .get("swiyu_tms_partner_link_by_type")
                .tag("type", TrustStatementTypeDto.ISSUANCE_V1.name())
                .gauge()
                .value()
        ).isEqualTo(2.0);
    }
}
