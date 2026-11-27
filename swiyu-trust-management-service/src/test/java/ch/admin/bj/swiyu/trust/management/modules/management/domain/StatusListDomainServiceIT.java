package ch.admin.bj.swiyu.trust.management.modules.management.domain;

import static ch.admin.bj.swiyu.trust.management.modules.management.domain.TrustStatementPartnerLink.createIdentityV2;
import static org.assertj.core.api.Assertions.assertThat;

import ch.admin.bit.jeap.security.test.WithJeapAuthenticationToken;
import ch.admin.bj.swiyu.trust.management.modules.management.config.issuer.IssuerJwtConfig;
import ch.admin.bj.swiyu.trust.management.modules.management.config.issuer.IssuerJwtProperties;
import ch.admin.bj.swiyu.trust.management.modules.management.config.statements.DefaultStatementProperties;
import ch.admin.bj.swiyu.trust.management.modules.management.domain.corebusiness.IssuerTrustRootProperties;
import ch.admin.bj.swiyu.trust.management.modules.management.domain.corebusiness.StatusRegistryProperties;
import ch.admin.bj.swiyu.trust.management.modules.management.domain.details.IdentityV2Details;
import ch.admin.bj.swiyu.trust.management.modules.registry.service.JsonJwtDeserializer;
import ch.admin.bj.swiyu.trust.management.test.AsyncTestConfig;
import ch.admin.bj.swiyu.trust.management.test.DataJpaTestConfiguration;
import ch.admin.bj.swiyu.trust.management.test.PostgreSQLContainerInitializer;
import ch.admin.bj.swiyu.trust.management.test.StatusListServiceTestConfiguration;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import lombok.extern.slf4j.Slf4j;
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
import org.testcontainers.junit.jupiter.Testcontainers;

@Slf4j
@Testcontainers
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@ContextConfiguration(initializers = PostgreSQLContainerInitializer.class)
@DataJpaTest
@Import(
    {
        StatusListDomainService.class,
        StatusListDomainService.class,
        IssuerJwtConfig.class,
        JwtStatementDomainService.class,
        JsonJwtDeserializer.class,
        DataJpaTestConfiguration.class,
        StatusListServiceTestConfiguration.class,
        AsyncTestConfig.class,
    }
)
@ActiveProfiles("test")
@EnableConfigurationProperties(
    {
        IssuerJwtProperties.class,
        DefaultStatementProperties.class,
        StatusRegistryProperties.class,
        IssuerTrustRootProperties.class,
    }
)
@TestPropertySource(properties = { "app.statement-defaults.statuslist.size=10" })
class StatusListDomainServiceIT {

    @Autowired
    StatusListDomainService statusListDomainService;

    @Autowired
    TrustStatementPartnerLinkRepository trustStatementPartnerLinkRepository;

    @Autowired
    StatusListMetadataRepository statusListMetadataRepository;

    @Autowired
    DefaultStatementProperties defaultStatementProperties;

    @Autowired
    JwtStatementDomainService jwtStatementDomainService;

    @BeforeEach
    void setUp() {
        trustStatementPartnerLinkRepository.deleteAllInBatch();
        statusListMetadataRepository.deleteAllInBatch();
    }

    @Test
    @WithJeapAuthenticationToken(username = "test")
    void rolloverOfStatusListIfFull() {
        // GIVEN
        for (var counter = 0; counter < (defaultStatementProperties.statuslist().size() * 3) - 1; counter++) {
            // WHEN
            var entry = statusListDomainService.getNewStatusListEntry();
            // THEN
            assertThat(entry.allocatedIndex()).isLessThan(defaultStatementProperties.statuslist().size());
        }
        var all = statusListMetadataRepository.findAll();
        assertThat(all).hasSize(3);
        assertThat(
            all.stream().filter(statusListMetadata -> statusListMetadata.getStatus() == StatusListMetadataStatus.FULL)
        ).hasSize(2);
    }

    @Test
    @WithJeapAuthenticationToken(username = "test")
    void publishedStatusListRevokesOnlyTheInactiveIndices() throws Exception {
        // GIVEN a status list whose entries are allocated the way statements are created
        var statementOnIndex0 = publishedStatement(statusListDomainService.getNewStatusListEntry());
        var statementOnIndex1 = publishedStatement(statusListDomainService.getNewStatusListEntry());
        trustStatementPartnerLinkRepository.saveAllAndFlush(List.of(statementOnIndex0, statementOnIndex1));
        // AND one of them is revoked afterwards
        statementOnIndex1.markAsInactive();
        trustStatementPartnerLinkRepository.saveAndFlush(statementOnIndex1);
        var statusListMetadata = statusListMetadataRepository
            .findById(statementOnIndex0.getStatusListMetadataId())
            .orElseThrow();

        // WHEN the status list is generated the way a publication does it
        var indices = trustStatementPartnerLinkRepository.findStatusListIndicesByStatusListMetadataIdAndStatus(
            statusListMetadata.getId(),
            TrustStatementPartnerLinkStatus.INACTIVE
        );
        var claims = jwtStatementDomainService
            .generateTokenStatusList(statusListMetadata, indices)
            .getJWTClaimsSet()
            .getJSONObjectClaim("status_list");
        var bits = ((Number) claims.get("bits")).intValue();
        var token = TokenStatusListToken.loadTokenStatusListToken(bits, (String) claims.get("lst"));

        // THEN only the inactive index is revoked
        assertThat(indices).containsExactly(statementOnIndex1.getStatusListIndex());
        assertThat(token.getStatus(statementOnIndex1.getStatusListIndex())).isEqualTo(
            TokenStatusListBit.REVOKE.getValue()
        );
        assertThat(token.getStatus(statementOnIndex0.getStatusListIndex())).isEqualTo(0);
    }

    private static TrustStatementPartnerLink publishedStatement(StatusListEntry statusListEntry) {
        var statement = createIdentityV2(
            UUID.randomUUID(),
            "did:status-list",
            Instant.parse("2025-01-01T00:00:00Z"),
            Instant.parse("2026-01-01T00:00:00Z"),
            Map.of("en", "Name EN"),
            List.of(new IdentityV2Details.RegistryId("registryType1", "registryValue1")),
            false,
            statusListEntry
        );
        // statements are created CONFIRMED and become active once they are published to the registry
        statement.persistReferencesAfterPublicationSucceeded(
            UUID.randomUUID(),
            UUID.randomUUID(),
            TrustStatementPartnerLinkStatus.ACTIVE
        );
        return statement;
    }
}
