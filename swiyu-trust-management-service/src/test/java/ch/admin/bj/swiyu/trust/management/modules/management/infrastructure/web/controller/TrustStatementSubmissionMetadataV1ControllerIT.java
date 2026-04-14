package ch.admin.bj.swiyu.trust.management.modules.management.infrastructure.web.controller;

import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import ch.admin.bit.jeap.security.test.WithJeapAuthenticationToken;
import ch.admin.bj.swiyu.trust.client.issuer.management.api.CredentialApi;
import ch.admin.bj.swiyu.trust.client.issuer.management.model.CredentialStatusTypeDto;
import ch.admin.bj.swiyu.trust.client.issuer.oid4vci.api.IssuerOid4VciApi;
import ch.admin.bj.swiyu.trust.management.modules.common.auth.UserRoles;
import ch.admin.bj.swiyu.trust.management.test.IssuerTestData;
import ch.admin.bj.swiyu.trust.management.test.PostgreSQLContainerInitializer;
import ch.admin.bj.swiyu.trust.management.test.RequestTestData;
import java.time.Instant;
import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;
import org.testcontainers.junit.jupiter.Testcontainers;

@ActiveProfiles("test")
@SpringBootTest(properties = { "kafka.enable=false" })
@AutoConfigureMockMvc
@Testcontainers
@ContextConfiguration(initializers = PostgreSQLContainerInitializer.class)
class TrustStatementPartnerLinkMetadataV1ControllerIT {

    @MockitoBean
    CredentialApi issuerManagementApi;

    @MockitoBean
    IssuerOid4VciApi issuerOid4vciApi;

    @Autowired
    private MockMvc mockMvc;

    @Test
    @WithJeapAuthenticationToken(userRoles = { UserRoles.EDITOR })
    void createSubmission_issuance_directConfirmFlow() throws Exception {
        // Given
        whenIssuerReturnsOk();
        var request = RequestTestData.tsMetadataV1Request(Map.of("en", "test"), Map.of("en", "test"), "en");

        // When
        mockMvc
            .perform(
                MockMvcRequestBuilders.post("/api/v1/trust-statement-partner-links/metadata")
                    .content(RequestTestData.objectMapper.writeValueAsString(request))
                    .contentType(MediaType.APPLICATION_JSON)
            )
            // Then
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.status").value("ACTIVE"))
            .andExpect(jsonPath("$.trustRegistryStatus").value("ACTIVE"))
            .andExpect(jsonPath("$.vcStatus").value("VALID"));
    }

    @ParameterizedTest
    @ValueSource(strings = { "en", "de-CH", "rm-CH", "it-CH", "fr-CH" })
    @WithJeapAuthenticationToken(userRoles = { UserRoles.EDITOR })
    void createSubmission_issuance_withoutLogoUris(String language) throws Exception {
        // Given
        whenIssuerReturnsOk();
        var request = RequestTestData.tsMetadataV1Request(Map.of(language, "test"), null, language);

        // When
        mockMvc
            .perform(
                post("/api/v1/trust-statement-partner-links/metadata")
                    .content(RequestTestData.objectMapper.writeValueAsString(request))
                    .contentType(MediaType.APPLICATION_JSON)
            )
            // Then
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.status").value("ACTIVE"))
            .andExpect(jsonPath("$.trustRegistryStatus").value("ACTIVE"))
            .andExpect(jsonPath("$.vcStatus").value("VALID"));
    }

    @ParameterizedTest
    @ValueSource(strings = { "en", "de-CH", "rm-CH", "it-CH", "fr-CH" })
    @WithJeapAuthenticationToken(userRoles = { UserRoles.EDITOR })
    void createSubmission_issuance_missingPreferredLanguage(String language) throws Exception {
        // Given
        whenIssuerReturnsOk();
        var request = RequestTestData.tsMetadataV1Request(
            Map.of(("en".compareTo(language) == 0 ? "de-CH" : "en"), "test"),
            Map.of(("en".compareTo(language) == 0 ? "de-CH" : "en"), "test"),
            language
        );

        // When
        mockMvc
            .perform(
                post("/api/v1/trust-statement-partner-links/metadata")
                    .content(RequestTestData.objectMapper.writeValueAsString(request))
                    .contentType(MediaType.APPLICATION_JSON)
            )
            // Then
            .andExpect(status().isBadRequest())
            .andExpect(
                jsonPath("$.globalErrors.[*].codes.[*]").value(
                    containsInAnyOrder("missing.logoUri.for.preferredLanguage", "missing.orgName.for.preferredLanguage")
                )
            );
    }

    @Test
    @WithJeapAuthenticationToken(userRoles = { UserRoles.EDITOR })
    void createSubmission_issuance_mixupValidDates() throws Exception {
        // Given
        whenIssuerReturnsOk();
        var request = RequestTestData.tsMetadataV1Request(
            Map.of("en", "test"),
            Map.of("en", "test"),
            "en",
            "did:example:something",
            Instant.now(),
            Instant.now().minusSeconds(60)
        );

        // When
        mockMvc
            .perform(
                post("/api/v1/trust-statement-partner-links/metadata")
                    .content(RequestTestData.objectMapper.writeValueAsString(request))
                    .contentType(MediaType.APPLICATION_JSON)
            )
            // Then
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.globalErrors.[0].codes").value("trust-statement.create.errors.timeframe-invalid"));
    }

    private void whenIssuerReturnsOk() {
        when(issuerManagementApi.createCredential(any())).thenReturn(IssuerTestData.credentialWithDeeplinkResponse());
        when(issuerOid4vciApi.oauthAccessToken(any(), any())).thenReturn(IssuerTestData.oAuthToken());
        when(issuerOid4vciApi.createCredential(any(), any())).thenReturn(IssuerTestData.credentialResponseAsString());
        when(issuerManagementApi.getCredentialStatus(any())).thenReturn(IssuerTestData.statusResponse_Issued());
        when(issuerManagementApi.updateCredentialStatus(any(), any())).thenAnswer(invocation ->
            IssuerTestData.updateStatusResponse(
                invocation.getArgument(0, UUID.class),
                invocation.getArgument(1, CredentialStatusTypeDto.class)
            )
        );
    }
}
