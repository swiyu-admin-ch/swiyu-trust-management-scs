package ch.admin.bj.swiyu.trust.management.modules.management.infrastructure.web.controller;

import static ch.admin.bj.swiyu.trust.management.modules.common.auth.UserRole.Names.EDITOR;
import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import ch.admin.bit.jeap.security.test.WithJeapAuthenticationToken;
import ch.admin.bj.swiyu.trust.management.modules.management.domain.TrustStatementPartnerLinkRepository;
import ch.admin.bj.swiyu.trust.management.modules.management.domain.details.TrustStatementPartnerLinkType;
import ch.admin.bj.swiyu.trust.management.test.MockAuditPublisherTestConfiguration;
import ch.admin.bj.swiyu.trust.management.test.PostgreSQLContainerInitializer;
import ch.admin.bj.swiyu.trust.management.test.RequestTestData;
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
@Import(MockAuditPublisherTestConfiguration.class)
class TrustStatementPartnerLinkProtectedIssuanceAuthorizationV2ControllerIT {

    @Autowired
    TrustStatementPartnerLinkRepository trustStatementPartnerLinkRepository;

    @Autowired
    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        trustStatementPartnerLinkRepository.deleteAllInBatch();
    }

    @Test
    @WithJeapAuthenticationToken(userRoles = { EDITOR })
    void create_ProtectedIssuanceAuthorizationV2() throws Exception {
        // Given
        var request = RequestTestData.tsProtectedIssuanceAuthorizationV2Request(null, null, null, null);

        // When
        var requestActions = mockMvc.perform(
            MockMvcRequestBuilders.post("/api/v2/trust-statement-partner-links/issuance")
                .content(RequestTestData.objectMapper.writeValueAsString(request))
                .contentType(MediaType.APPLICATION_JSON)
        );

        // Then
        requestActions
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.status").value("ACTIVE"))
            .andExpect(jsonPath("$.trustRegistryStatus").value("ACTIVE"))
            .andExpect(jsonPath("$.vcStatus").value("VALID"));

        var statements = trustStatementPartnerLinkRepository.findAll();
        assertThat(statements).hasSize(1);
        assertThat(statements.get(0).getType()).isEqualTo(
            TrustStatementPartnerLinkType.TRUST_STATEMENT_PROTECTED_ISSUANCE_AUTHORIZATION_V2
        );
    }
}
