package ch.admin.bj.swiyu.trust.management.modules.management.service;

import static ch.admin.bj.swiyu.trust.management.modules.management.domain.details.TrustStatementPartnerLinkType.TRUST_LIST_STATEMENT_NON_COMPLIANCE_V2;
import static ch.admin.bj.swiyu.trust.management.test.NonCompliantActorTestData.nonCompliantActorRequestDto;
import static org.assertj.core.api.Assertions.assertThat;

import ch.admin.bj.swiyu.trust.management.modules.management.config.issuer.IssuerJwtConfig;
import ch.admin.bj.swiyu.trust.management.modules.management.config.issuer.IssuerJwtProperties;
import ch.admin.bj.swiyu.trust.management.modules.management.config.statements.DefaultStatementProperties;
import ch.admin.bj.swiyu.trust.management.modules.management.domain.JwtStatementDomainService;
import ch.admin.bj.swiyu.trust.management.modules.management.domain.TrustStatementPartnerLinkStatus;
import ch.admin.bj.swiyu.trust.management.modules.management.domain.TrustStatementPartnerLinkValidator;
import ch.admin.bj.swiyu.trust.management.modules.management.domain.corebusiness.IssuerTrustRootProperties;
import ch.admin.bj.swiyu.trust.management.modules.management.domain.details.NonComplianceV2Details;
import ch.admin.bj.swiyu.trust.management.modules.registry.service.JsonJwtDeserializer;
import ch.admin.bj.swiyu.trust.management.modules.registry.service.NonComplianceListService;
import ch.admin.bj.swiyu.trust.management.modules.registry.service.TrustRegistryService;
import ch.admin.bj.swiyu.trust.management.test.*;
import org.jspecify.annotations.NonNull;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.context.annotation.Import;
import org.springframework.data.domain.Sort;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.ContextConfiguration;
import org.testcontainers.junit.jupiter.Testcontainers;

@Testcontainers
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@ContextConfiguration(initializers = PostgreSQLContainerInitializer.class)
@DataJpaTest
@Import(
    {
        IssuerJwtConfig.class,
        DomainEventService.class,
        NonCompliantActorService.class,
        NonComplianceListService.class,
        NonCompliantActorPublicationService.class,
        JwtStatementDomainService.class,
        JsonJwtDeserializer.class,
        TrustStatementService.class,
        TrustRegistryService.class,
        TrustStatementPartnerLinkValidator.class,
        DataJpaTestConfiguration.class,
        StatusListServiceTestConfiguration.class,
        AsyncTestConfig.class,
        MockAuditPublisherTestConfiguration.class,
    }
)
@EnableConfigurationProperties(
    { IssuerJwtProperties.class, DefaultStatementProperties.class, IssuerTrustRootProperties.class }
)
@ActiveProfiles("test")
class NonCompliantActorPublicationServiceIT {

    private static final String CURRENT_USER = "Alice Admin";

    @Autowired
    private NonCompliantActorPublicationService nonCompliantActorPublicationService;

    @Autowired
    private NonCompliantActorService nonCompliantActorService;

    @Autowired
    private AsyncTestConfig asyncTestConfig;

    @Autowired
    private TestRepositories repos;

    @BeforeEach
    void setup() {
        repos.domainEventLog.deleteAllInBatch();
        repos.trustStatementPartnerLink.deleteAllInBatch();
        repos.nonCompliantActor.deleteAllInBatch();
        repos.nonComplianceList.deleteAllInBatch();
    }

    @Test
    void triggerPublicationAsync() {
        // given
        var req = nonCompliantActorRequestDto();
        var actor = nonCompliantActorService.createNonCompliantActor(req, CURRENT_USER);
        repos.commit();

        // when 1 (publication with actor)
        nonCompliantActorPublicationService.triggerPublicationAsync();
        asyncTestConfig.waitForAsyncOperationsFinished();

        // then 1
        var nonComplianceListV1 = repos.nonComplianceList.findAll(orderByPublishedAt()).getFirst();
        var nonComplianceListV2 = getCurrentNonComplianceListV2();
        assertThat(nonComplianceListV1).isNotNull();
        assertThat(nonComplianceListV2).isNotNull();
        assertThat(nonComplianceListV1.getPayload()).contains(req.did());
        assertThat(nonComplianceListV2.getNonCompliantActors())
            .extracting(NonComplianceV2Details.NonCompliantActor::actor)
            .contains(req.did());

        // when 2 (deleting actor and republish)
        nonCompliantActorService.deleteNonCompliantActor(actor.id(), CURRENT_USER);
        nonCompliantActorPublicationService.triggerPublicationAsync();
        asyncTestConfig.waitForAsyncOperationsFinished();

        // then 2
        nonComplianceListV1 = repos.nonComplianceList.findAll(orderByPublishedAt()).getFirst();
        nonComplianceListV2 = getCurrentNonComplianceListV2();
        assertThat(nonComplianceListV1).isNotNull();
        assertThat(nonComplianceListV1.getPayload()).doesNotContain(req.did());
        assertThat(nonComplianceListV2.getNonCompliantActors())
            .extracting(NonComplianceV2Details.NonCompliantActor::actor)
            .doesNotContain(req.did());
    }

    @Test
    void triggerPublication() {
        // given
        var req = nonCompliantActorRequestDto();
        var actor = nonCompliantActorService.createNonCompliantActor(req, CURRENT_USER);
        repos.commit();

        // when 1 (publication with actor)
        nonCompliantActorPublicationService.triggerPublication();

        // then 1
        var nonComplianceListV1 = repos.nonComplianceList.findAll(orderByPublishedAt()).getFirst();
        var nonComplianceListV2 = getCurrentNonComplianceListV2();
        assertThat(nonComplianceListV1).isNotNull();
        assertThat(nonComplianceListV2).isNotNull();
        assertThat(nonComplianceListV1.getPayload()).contains(req.did());
        assertThat(nonComplianceListV2.getNonCompliantActors())
            .extracting(NonComplianceV2Details.NonCompliantActor::actor)
            .contains(req.did());

        // when 2 (deleting actor and republish)
        nonCompliantActorService.deleteNonCompliantActor(actor.id(), CURRENT_USER);
        nonCompliantActorPublicationService.triggerPublication();

        // then 2
        nonComplianceListV1 = repos.nonComplianceList.findAll(orderByPublishedAt()).getFirst();
        nonComplianceListV2 = getCurrentNonComplianceListV2();
        assertThat(nonComplianceListV1).isNotNull();
        assertThat(nonComplianceListV1.getPayload()).doesNotContain(req.did());
        assertThat(nonComplianceListV2.getNonCompliantActors())
            .extracting(NonComplianceV2Details.NonCompliantActor::actor)
            .doesNotContain(req.did());
    }

    private NonComplianceV2Details getCurrentNonComplianceListV2() {
        var all = repos.trustStatementPartnerLink.findAllByTypeAndStatus(
            TRUST_LIST_STATEMENT_NON_COMPLIANCE_V2,
            TrustStatementPartnerLinkStatus.ACTIVE
        );
        assertThat(all).as("Expected at most one ACTIVE NonComplianceV2 statement").hasSizeLessThanOrEqualTo(1);
        return all.isEmpty() ? null : (NonComplianceV2Details) all.getFirst().getDetails();
    }

    private static @NonNull Sort orderByPublishedAt() {
        return Sort.by(Sort.Direction.DESC, "publishedAt");
    }
}
