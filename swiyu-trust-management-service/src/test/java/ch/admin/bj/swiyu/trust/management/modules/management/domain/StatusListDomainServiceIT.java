package ch.admin.bj.swiyu.trust.management.modules.management.domain;

import static org.assertj.core.api.Assertions.assertThat;

import ch.admin.bit.jeap.security.test.WithJeapAuthenticationToken;
import ch.admin.bj.swiyu.trust.management.modules.management.config.issuer.IssuerJwtConfig;
import ch.admin.bj.swiyu.trust.management.modules.management.config.issuer.IssuerJwtProperties;
import ch.admin.bj.swiyu.trust.management.modules.management.config.statements.DefaultStatementProperties;
import ch.admin.bj.swiyu.trust.management.modules.management.domain.corebusiness.IssuerTrustRootProperties;
import ch.admin.bj.swiyu.trust.management.modules.management.domain.corebusiness.StatusRegistryProperties;
import ch.admin.bj.swiyu.trust.management.modules.registry.service.JsonJwtDeserializer;
import ch.admin.bj.swiyu.trust.management.test.AsyncTestConfig;
import ch.admin.bj.swiyu.trust.management.test.DataJpaTestConfiguration;
import ch.admin.bj.swiyu.trust.management.test.PostgreSQLContainerInitializer;
import ch.admin.bj.swiyu.trust.management.test.StatusListServiceTestConfiguration;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
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
}
