package ch.admin.bj.swiyu.trust.management.modules.management.infrastructure.web.controller;

import static ch.admin.bj.swiyu.trust.management.modules.common.auth.UserRole.Names.EDITOR;
import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import ch.admin.bit.jeap.security.test.WithJeapAuthenticationToken;
import ch.admin.bj.swiyu.trust.management.modules.management.domain.ProtectedIssuanceEntryRepository;
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
class ProtectedIssuanceEntryControllerIT {

    @Autowired
    TrustStatementPartnerLinkRepository trustStatementPartnerLinkRepository;

    @Autowired
    ProtectedIssuanceEntryRepository protectedIssuanceEntryRepository;

    @Autowired
    AsyncTestConfig asyncTestConfig;

    @Autowired
    private MockMvc mockMvc;

    private void waitForTrustStatementGeneration(int expectedCount) {
        asyncTestConfig.waitForAsyncOperationsFinished();
        assertThat(trustStatementPartnerLinkRepository.findAll()).hasSize(expectedCount);
    }

    @BeforeEach
    void setUp() {
        asyncTestConfig.waitForAsyncOperationsFinished();
        trustStatementPartnerLinkRepository.deleteAllInBatch();
        protectedIssuanceEntryRepository.deleteAllInBatch();
    }

    @Test
    @WithJeapAuthenticationToken(userRoles = { EDITOR })
    void create_ProtectedIssuanceEntry() throws Exception {
        // Given
        var request = RequestTestData.tsProtectedIssuanceEntryCreateRequest("urn:vct:test");

        // When
        var requestActions = mockMvc.perform(
            MockMvcRequestBuilders.post("/api/v2/protected-issuance-entry")
                .content(RequestTestData.objectMapper.writeValueAsString(request))
                .contentType(MediaType.APPLICATION_JSON)
        );

        // Then
        requestActions
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.id").isNotEmpty())
            .andExpect(jsonPath("$.vct").value("urn:vct:test"));

        waitForTrustStatementGeneration(1);
        var statements = trustStatementPartnerLinkRepository.findAll();
        assertThat(statements).hasSize(1);
        assertThat(statements.get(0).getType()).isEqualTo(
            TrustStatementPartnerLinkType.TRUST_LIST_STATEMENT_PROTECTED_ISSUANCE_V2
        );
    }

    @Test
    @WithJeapAuthenticationToken(userRoles = { EDITOR })
    void get_ProtectedIssuanceEntry() throws Exception {
        // Given
        mockMvc.perform(
            MockMvcRequestBuilders.post("/api/v2/protected-issuance-entry")
                .contentType(MediaType.APPLICATION_JSON)
                .content(
                    RequestTestData.objectMapper.writeValueAsString(
                        RequestTestData.tsProtectedIssuanceEntryCreateRequest("urn:vct:test")
                    )
                )
        );
        var entry = protectedIssuanceEntryRepository.getByVct("urn:vct:test");

        // When
        var requestActions = mockMvc.perform(
            MockMvcRequestBuilders.get("/api/v2/protected-issuance-entry/{id}", entry.getId()).contentType(
                MediaType.APPLICATION_JSON
            )
        );

        // Then
        requestActions
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.id").value(entry.getId().toString()))
            .andExpect(jsonPath("$.vct").value("urn:vct:test"));
    }

    @Test
    @WithJeapAuthenticationToken(userRoles = { EDITOR })
    void list_ProtectedIssuanceEntry_filterByVct() throws Exception {
        // Given
        mockMvc.perform(
            MockMvcRequestBuilders.post("/api/v2/protected-issuance-entry")
                .contentType(MediaType.APPLICATION_JSON)
                .content(
                    RequestTestData.objectMapper.writeValueAsString(
                        RequestTestData.tsProtectedIssuanceEntryCreateRequest("urn:vct:test1")
                    )
                )
                .contentType(MediaType.APPLICATION_JSON)
        );
        mockMvc.perform(
            MockMvcRequestBuilders.post("/api/v2/protected-issuance-entry")
                .contentType(MediaType.APPLICATION_JSON)
                .content(
                    RequestTestData.objectMapper.writeValueAsString(
                        RequestTestData.tsProtectedIssuanceEntryCreateRequest("urn:vct:test2")
                    )
                )
        );

        // When
        var requestActions = mockMvc.perform(
            MockMvcRequestBuilders.get("/api/v2/protected-issuance-entry/?vct={vct}", "urn:vct:test2").contentType(
                MediaType.APPLICATION_JSON
            )
        );

        // Then
        requestActions
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.page.totalElements").value(1))
            .andExpect(jsonPath("$.page.totalPages").value(1))
            .andExpect(jsonPath("$.content").isArray())
            .andExpect(jsonPath("$.content.[0].vct").value("urn:vct:test2"));
    }

    @Test
    @WithJeapAuthenticationToken(userRoles = { EDITOR })
    void list_ProtectedIssuanceEntry() throws Exception {
        // Given
        mockMvc.perform(
            MockMvcRequestBuilders.post("/api/v2/protected-issuance-entry")
                .contentType(MediaType.APPLICATION_JSON)
                .content(
                    RequestTestData.objectMapper.writeValueAsString(
                        RequestTestData.tsProtectedIssuanceEntryCreateRequest("urn:vct:test1")
                    )
                )
                .contentType(MediaType.APPLICATION_JSON)
        );
        mockMvc.perform(
            MockMvcRequestBuilders.post("/api/v2/protected-issuance-entry")
                .contentType(MediaType.APPLICATION_JSON)
                .content(
                    RequestTestData.objectMapper.writeValueAsString(
                        RequestTestData.tsProtectedIssuanceEntryCreateRequest("urn:vct:test2")
                    )
                )
        );

        // When
        var requestActions = mockMvc.perform(
            MockMvcRequestBuilders.get("/api/v2/protected-issuance-entry/").contentType(MediaType.APPLICATION_JSON)
        );

        // Then
        requestActions
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.page.totalElements").value(2))
            .andExpect(jsonPath("$.page.totalPages").value(1))
            .andExpect(jsonPath("$.content").isArray());
    }

    @Test
    @WithJeapAuthenticationToken(userRoles = { EDITOR })
    void delete_ProtectedIssuanceEntry() throws Exception {
        // Given
        mockMvc
            .perform(
                MockMvcRequestBuilders.post("/api/v2/protected-issuance-entry")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(
                        RequestTestData.objectMapper.writeValueAsString(
                            RequestTestData.tsProtectedIssuanceEntryCreateRequest("urn:vct:test")
                        )
                    )
            )
            .andExpect(status().isCreated());
        waitForTrustStatementGeneration(1);
        var entry = protectedIssuanceEntryRepository.getByVct("urn:vct:test");

        // When
        var requestActions = mockMvc.perform(
            MockMvcRequestBuilders.delete("/api/v2/protected-issuance-entry/{id}", entry.getId()).contentType(
                MediaType.APPLICATION_JSON
            )
        );

        // Then
        requestActions.andExpect(status().isOk());

        waitForTrustStatementGeneration(2);
        var statements = trustStatementPartnerLinkRepository.findAll();
        assertThat(statements).hasSize(2);
        assertThat(statements.get(0).getType()).isEqualTo(
            TrustStatementPartnerLinkType.TRUST_LIST_STATEMENT_PROTECTED_ISSUANCE_V2
        );
        assertThat(statements.stream().filter(s -> s.getStatus() == TrustStatementPartnerLinkStatus.ACTIVE)).hasSize(1);
    }
}
