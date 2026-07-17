package ch.admin.bj.swiyu.trust.management.modules.management.infrastructure.web.controller;

import static ch.admin.bj.swiyu.trust.management.modules.common.auth.UserRole.Names.EDITOR;
import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import ch.admin.bit.jeap.security.test.WithJeapAuthenticationToken;
import ch.admin.bj.swiyu.trust.management.modules.management.domain.StatusListMetadataRepository;
import ch.admin.bj.swiyu.trust.management.modules.management.domain.TrustStatementPartnerLinkRepository;
import ch.admin.bj.swiyu.trust.management.modules.management.domain.details.TrustStatementPartnerLinkType;
import ch.admin.bj.swiyu.trust.management.modules.registry.domain.StatementRepository;
import ch.admin.bj.swiyu.trust.management.modules.registry.domain.StatementType;
import ch.admin.bj.swiyu.trust.management.test.AsyncTestConfig;
import ch.admin.bj.swiyu.trust.management.test.MockAuditPublisherTestConfiguration;
import ch.admin.bj.swiyu.trust.management.test.PostgreSQLContainerInitializer;
import ch.admin.bj.swiyu.trust.management.test.RequestTestData;
import ch.admin.bj.swiyu.trust.management.test.StatusListServiceTestConfiguration;
import com.nimbusds.jwt.SignedJWT;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;
import org.testcontainers.junit.jupiter.Testcontainers;

@ActiveProfiles("test")
@SpringBootTest(properties = { "kafka.enable=false" })
@AutoConfigureMockMvc
@Testcontainers
@ContextConfiguration(initializers = PostgreSQLContainerInitializer.class)
@Import({ StatusListServiceTestConfiguration.class, MockAuditPublisherTestConfiguration.class })
class TrustStatementPartnerLinkIdentityV2ControllerIT {

    @Autowired
    TrustStatementPartnerLinkRepository trustStatementPartnerLinkRepository;

    @Autowired
    StatementRepository statementRepository;

    @Autowired
    StatusListMetadataRepository statusListMetadataRepository;

    @Autowired
    AsyncTestConfig asyncTestConfig;

    @Autowired
    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        asyncTestConfig.waitForAsyncOperationsFinished();
        statementRepository.deleteAllInBatch();
        trustStatementPartnerLinkRepository.deleteAllInBatch();
        statusListMetadataRepository.deleteAllInBatch();
    }

    @Test
    @WithJeapAuthenticationToken(userRoles = { EDITOR })
    void create_IdentityV2() throws Exception {
        // Given
        var request = RequestTestData.tsIdentityV2RequestDto();

        // When
        var requestActions = mockMvc.perform(
            MockMvcRequestBuilders.post("/api/v2/trust-statement-partner-links/identity")
                .content(RequestTestData.objectMapper.writeValueAsString(request))
                .contentType(MediaType.APPLICATION_JSON)
        );

        // Then
        requestActions
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.status").value("ACTIVE"))
            .andExpect(jsonPath("$.trustRegistryStatus").value("ACTIVE"))
            .andExpect(jsonPath("$.vcStatus").value("VALID"));

        var partnerLinks = trustStatementPartnerLinkRepository.findAll();
        assertThat(partnerLinks).hasSize(1);
        assertThat(partnerLinks.get(0).getType()).isEqualTo(TrustStatementPartnerLinkType.TRUST_STATEMENT_IDENTITY_V2);

        var statements = statementRepository.findAll();
        assertThat(statements).hasSize(1);
        assertThat(statements.get(0).getType()).isEqualTo(StatementType.IDENTITY_TRUST_STATEMENT_V2);
        var rawToken = statements.get(0).getSerialized();
        var token = SignedJWT.parse(rawToken);
        var status = (Map<String, Map<String, Object>>) token.getJWTClaimsSet().getClaim("status");
        assertThat(status).describedAs(rawToken).isNotNull().containsKey("status_list");
        assertThat(status.get("status_list")).containsKeys("idx", "uri");
        assertThat(status.get("status_list").get("idx")).isEqualTo(0L);
        assertThat(status.get("status_list").get("uri")).isInstanceOfSatisfying(String.class, uri ->
            assertThat(uri).startsWith("https://status.list.mock/")
        );
    }
}
