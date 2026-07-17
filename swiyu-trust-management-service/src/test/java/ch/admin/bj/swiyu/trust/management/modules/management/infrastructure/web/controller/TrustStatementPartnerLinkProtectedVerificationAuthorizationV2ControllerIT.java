package ch.admin.bj.swiyu.trust.management.modules.management.infrastructure.web.controller;

import static ch.admin.bj.swiyu.trust.management.modules.common.auth.UserRole.Names.EDITOR;
import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import ch.admin.bit.jeap.security.test.WithJeapAuthenticationToken;
import ch.admin.bj.swiyu.trust.management.modules.management.domain.TrustStatementPartnerLinkRepository;
import ch.admin.bj.swiyu.trust.management.test.AsyncTestConfig;
import ch.admin.bj.swiyu.trust.management.test.MockAuditPublisherTestConfiguration;
import ch.admin.bj.swiyu.trust.management.test.PostgreSQLContainerInitializer;
import ch.admin.bj.swiyu.trust.management.test.RequestTestData;
import ch.admin.bj.swiyu.trust.management.test.StatusListServiceTestConfiguration;
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
class TrustStatementPartnerLinkProtectedVerificationAuthorizationV2ControllerIT {

    @Autowired
    TrustStatementPartnerLinkRepository trustStatementPartnerLinkRepository;

    @Autowired
    AsyncTestConfig asyncTestConfig;

    @Autowired
    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        asyncTestConfig.waitForAsyncOperationsFinished();
        trustStatementPartnerLinkRepository.deleteAllInBatch();
    }

    @Test
    @WithJeapAuthenticationToken(userRoles = { EDITOR })
    void create_ProtectedVerificationAuthorizationV2() throws Exception {
        // Given
        var request = RequestTestData.tsProtectedVerificationAuthorizationV2Request(null, null, null, null);

        // When
        var requestActions = mockMvc.perform(
            MockMvcRequestBuilders.post("/api/v2/trust-statement-partner-links/verification")
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
    }
}
