package ch.admin.bj.swiyu.trust.management.modules.management.infrastructure.web.controller;

import static ch.admin.bj.swiyu.trust.management.modules.common.persistence.TransactionManagerNames.MANAGEMENT_TRANSACTION_MANAGER;
import static ch.admin.bj.swiyu.trust.management.test.RequestTestData.BUSINESS_PARTNER_A_ID;
import static ch.admin.bj.swiyu.trust.management.test.TrustStatementTestData.issuanceV1;
import static ch.admin.bj.swiyu.trust.management.test.TrustStatementTestData.issuanceV1Request;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;

import ch.admin.bit.jeap.security.test.WithJeapAuthenticationToken;
import ch.admin.bj.swiyu.trust.client.issuer.management.api.CredentialApi;
import ch.admin.bj.swiyu.trust.client.issuer.management.model.CredentialStatusTypeDto;
import ch.admin.bj.swiyu.trust.client.issuer.oid4vci.api.IssuerOid4VciApi;
import ch.admin.bj.swiyu.trust.management.modules.common.auth.UserRoles;
import ch.admin.bj.swiyu.trust.management.modules.management.domain.TrustStatementPartnerLinkRepository;
import ch.admin.bj.swiyu.trust.management.modules.management.service.TrustStatementService;
import ch.admin.bj.swiyu.trust.management.test.IssuerTestData;
import ch.admin.bj.swiyu.trust.management.test.MockAuditPublisherTestConfiguration;
import ch.admin.bj.swiyu.trust.management.test.PostgreSQLContainerInitializer;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.context.transaction.TestTransaction;
import org.springframework.test.web.servlet.assertj.MockMvcTester;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;
import org.springframework.transaction.annotation.Transactional;
import org.testcontainers.junit.jupiter.Testcontainers;

@ActiveProfiles("test")
@SpringBootTest
@AutoConfigureMockMvc
@Testcontainers
@ContextConfiguration(initializers = PostgreSQLContainerInitializer.class)
@Import(MockAuditPublisherTestConfiguration.class)
class TrustStatementPartnerLinkControllerIT {

    @MockitoBean
    CredentialApi issuerManagementApi;

    @MockitoBean
    IssuerOid4VciApi issuerOid4vciApi;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private MockMvcTester mockMvcTester = MockMvcTester.of();

    @Autowired
    private TrustStatementPartnerLinkRepository trustStatementPartnerLinkRepository;

    @Autowired
    private TrustStatementService trustStatementService;

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

    @BeforeEach
    @Transactional(transactionManager = MANAGEMENT_TRANSACTION_MANAGER)
    void setUp() {
        trustStatementPartnerLinkRepository.deleteAllInBatch();
    }

    @Test
    @Transactional(transactionManager = MANAGEMENT_TRANSACTION_MANAGER)
    @WithJeapAuthenticationToken(userRoles = { UserRoles.READER })
    void getSubmissions() {
        // Given
        trustStatementPartnerLinkRepository.saveAllAndFlush(List.of(issuanceV1("did:1"), issuanceV1("did:2")));
        TestTransaction.flagForCommit();
        TestTransaction.end();

        // When
        var result = mockMvcTester.perform(get("/api/v1/trust-statement-partner-links/"));

        // Then
        assertThat(result).hasStatus(HttpStatus.OK);
        assertThat(result).bodyJson().extractingPath("$.page.totalElements").isEqualTo(2);
    }

    @Test
    @WithJeapAuthenticationToken(userRoles = { UserRoles.EDITOR })
    void deactivateSubmission() {
        // GIVEN
        whenIssuerReturnsOk();
        var statement = trustStatementService.issueAndPublishIssuanceV1TrustStatement(
            BUSINESS_PARTNER_A_ID,
            issuanceV1Request("did:1")
        );

        // WHEN
        var result = mockMvcTester.perform(
            delete("/api/v1/trust-statement-partner-links/" + statement.getId())
                .content(
                    """
                    {
                      "reason": "testrun"
                    }"""
                )
                .contentType(MediaType.APPLICATION_JSON)
        );
        // THEN
        assertThat(result).hasStatus(HttpStatus.OK).bodyJson().extractingPath("$.status").isEqualTo("INACTIVE");
    }

    @Test
    @WithJeapAuthenticationToken(userRoles = { UserRoles.READER })
    void getSubmission_NotFound() {
        // GIVEN
        var id = UUID.randomUUID();

        // When
        var result = mockMvcTester.perform(
            MockMvcRequestBuilders.get("/api/v1/trust-statement-partner-links/" + id).contentType(
                MediaType.APPLICATION_JSON
            )
        );

        // Then
        result.assertThat().hasStatus(HttpStatus.NOT_FOUND);
    }
}
