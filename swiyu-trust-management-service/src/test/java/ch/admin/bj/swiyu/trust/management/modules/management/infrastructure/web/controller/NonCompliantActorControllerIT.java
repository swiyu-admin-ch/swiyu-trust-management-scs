package ch.admin.bj.swiyu.trust.management.modules.management.infrastructure.web.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import ch.admin.bit.jeap.security.test.WithJeapAuthenticationToken;
import ch.admin.bj.swiyu.trust.management.modules.common.auth.UserRoles;
import ch.admin.bj.swiyu.trust.management.modules.management.domain.NonCompliantActorRepository;
import ch.admin.bj.swiyu.trust.management.modules.management.domain.TrustStatementPartnerLinkRepository;
import ch.admin.bj.swiyu.trust.management.modules.management.domain.TrustStatementPartnerLinkStatus;
import ch.admin.bj.swiyu.trust.management.modules.management.domain.details.TrustStatementPartnerLinkType;
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
class NonCompliantActorControllerIT {

    @Autowired
    TrustStatementPartnerLinkRepository trustStatementPartnerLinkRepository;

    @Autowired
    NonCompliantActorRepository nonCompliantActorRepository;

    @Autowired
    AsyncTestConfig asyncTestConfig;

    @Autowired
    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        asyncTestConfig.waitForAsyncOperationsFinished();
        nonCompliantActorRepository.deleteAllInBatch();
        trustStatementPartnerLinkRepository.deleteAllInBatch();
    }

    @Test
    @WithJeapAuthenticationToken(userRoles = { UserRoles.EDITOR })
    void create_NonComplianceActor() throws Exception {
        // Given
        var request = RequestTestData.tsNonComplianceActorCreateRequest(null);

        // When
        var requestActions = mockMvc.perform(
            MockMvcRequestBuilders.post("/api/v1/non-compliant-actors")
                .content(RequestTestData.objectMapper.writeValueAsString(request))
                .contentType(MediaType.APPLICATION_JSON)
        );

        // Then
        requestActions.andExpect(status().isCreated()).andExpect(jsonPath("$.id").isNotEmpty());

        asyncTestConfig.waitForAsyncOperationsFinished();
        var statements = trustStatementPartnerLinkRepository.findAll();
        assertThat(statements).hasSize(1);
        assertThat(statements.get(0).getType()).isEqualTo(
            TrustStatementPartnerLinkType.TRUST_LIST_STATEMENT_NON_COMPLIANCE_V2
        );
    }

    @Test
    @WithJeapAuthenticationToken(userRoles = { UserRoles.EDITOR })
    void get_NonComplianceActor() throws Exception {
        // Given
        mockMvc.perform(
            MockMvcRequestBuilders.post("/api/v1/non-compliant-actors")
                .contentType(MediaType.APPLICATION_JSON)
                .content(
                    RequestTestData.objectMapper.writeValueAsString(
                        RequestTestData.tsNonComplianceActorCreateRequest("did:test:abc")
                    )
                )
        );
        var entry = nonCompliantActorRepository.findNonCompliantActorByDid("did:test:abc").get();

        // When
        var requestActions = mockMvc.perform(
            MockMvcRequestBuilders.get("/api/v1/non-compliant-actors/{id}", entry.getId()).contentType(
                MediaType.APPLICATION_JSON
            )
        );

        // Then
        requestActions.andExpect(status().isOk()).andExpect(jsonPath("$.id").value(entry.getId().toString()));
    }

    @Test
    @WithJeapAuthenticationToken(userRoles = { UserRoles.EDITOR })
    void list_NonComplianceActor() throws Exception {
        // Given
        mockMvc.perform(
            MockMvcRequestBuilders.post("/api/v1/non-compliant-actors")
                .contentType(MediaType.APPLICATION_JSON)
                .content(
                    RequestTestData.objectMapper.writeValueAsString(
                        RequestTestData.tsNonComplianceActorCreateRequest("did:test1")
                    )
                )
                .contentType(MediaType.APPLICATION_JSON)
        );
        mockMvc.perform(
            MockMvcRequestBuilders.post("/api/v1/non-compliant-actors")
                .contentType(MediaType.APPLICATION_JSON)
                .content(
                    RequestTestData.objectMapper.writeValueAsString(
                        RequestTestData.tsNonComplianceActorCreateRequest("did:test2")
                    )
                )
        );

        // When
        var requestActions = mockMvc.perform(
            MockMvcRequestBuilders.get("/api/v1/non-compliant-actors").contentType(MediaType.APPLICATION_JSON)
        );

        // Then
        requestActions
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.page.totalElements").value(2))
            .andExpect(jsonPath("$.page.totalPages").value(1))
            .andExpect(jsonPath("$.content").isArray());
    }

    @Test
    @WithJeapAuthenticationToken(userRoles = { UserRoles.EDITOR })
    void delete_NonComplianceActor() throws Exception {
        // Given
        mockMvc
            .perform(
                MockMvcRequestBuilders.post("/api/v1/non-compliant-actors")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(
                        RequestTestData.objectMapper.writeValueAsString(
                            RequestTestData.tsNonComplianceActorCreateRequest("did:test")
                        )
                    )
            )
            .andExpect(status().isCreated());
        asyncTestConfig.waitForAsyncOperationsFinished();
        var entry = nonCompliantActorRepository.findNonCompliantActorByDid("did:test").get();

        // When
        var requestActions = mockMvc.perform(
            MockMvcRequestBuilders.delete("/api/v1/non-compliant-actors/{id}", entry.getId()).contentType(
                MediaType.APPLICATION_JSON
            )
        );

        // Then
        requestActions.andExpect(status().isOk());

        asyncTestConfig.waitForAsyncOperationsFinished();
        var statements = trustStatementPartnerLinkRepository.findAll();
        assertThat(statements).hasSize(2);
        assertThat(statements.get(0).getType()).isEqualTo(
            TrustStatementPartnerLinkType.TRUST_LIST_STATEMENT_NON_COMPLIANCE_V2
        );
        assertThat(statements.stream().filter(s -> s.getStatus() == TrustStatementPartnerLinkStatus.ACTIVE)).hasSize(1);
    }
}
