package ch.admin.bj.swiyu.trust.management.modules.management.infrastructure.web.controller;

import static ch.admin.bj.swiyu.trust.management.modules.common.auth.UserRole.Names.EDITOR;
import static ch.admin.bj.swiyu.trust.management.modules.common.persistence.TransactionManagerNames.MANAGEMENT_TRANSACTION_MANAGER;
import static ch.admin.bj.swiyu.trust.management.test.BusinessPartnerIdentityTestData.newDefaultBusinessPartnerIdentity;
import static ch.admin.bj.swiyu.trust.management.test.ProtectedVerificationAuthorizationTestData.createProtectedVerificationAuthorizationRequest;
import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;
import static org.mockito.Mockito.verify;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import ch.admin.bit.jeap.security.test.WithJeapAuthenticationToken;
import ch.admin.bj.swiyu.messagetype.ti.TiBusinessPartnerIdentityUpdatedEvent;
import ch.admin.bj.swiyu.trust.management.modules.management.domain.publisher.OutboxEventPublisher;
import ch.admin.bj.swiyu.trust.management.test.PostgreSQLContainerInitializer;
import ch.admin.bj.swiyu.trust.management.test.ProtectedVerificationAuthorizationTestData;
import ch.admin.bj.swiyu.trust.management.test.RequestTestData;
import ch.admin.bj.swiyu.trust.management.test.TestRepositories;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.kafka.test.context.EmbeddedKafka;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.context.transaction.TestTransaction;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;
import org.springframework.transaction.annotation.Transactional;
import org.testcontainers.junit.jupiter.Testcontainers;

@ActiveProfiles("test")
@SpringBootTest
@EmbeddedKafka
@AutoConfigureMockMvc
@Testcontainers
@ContextConfiguration(initializers = PostgreSQLContainerInitializer.class)
class ProtectedVerificationAuthorizationControllerIT {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private TestRepositories repos;

    @MockitoBean
    private OutboxEventPublisher outboxEventPublisher;

    private void verifyBusinessPartnerIdentityUpdateEventIsEmitted(UUID bpiId) {
        var bpiUpdatedCaptor = ArgumentCaptor.forClass(TiBusinessPartnerIdentityUpdatedEvent.class);
        verify(outboxEventPublisher).publishBusinessPartnerIdentityUpdatedEvent(bpiUpdatedCaptor.capture());
        var updatedEvent = bpiUpdatedCaptor.getValue();
        assertThat(updatedEvent).isNotNull();
        assertThat(updatedEvent.getPayload()).isNotNull();
        var updatedPayload = updatedEvent.getPayload();
        assertThat(updatedPayload.getBusinessPartnerIdentityId()).isEqualTo(bpiId);
    }

    @BeforeEach
    void setUp() {
        repos.businessPartnerIdentityRepository.deleteAllInBatch();
        repos.protectedVerificationRepository.deleteAllInBatch();
        repos.trustStatementPartnerLink.deleteAllInBatch();
    }

    @Test
    @Transactional(transactionManager = MANAGEMENT_TRANSACTION_MANAGER)
    @WithJeapAuthenticationToken(userRoles = { EDITOR })
    void addProtectedVerificationAuthorization() throws Exception {
        // Given
        repos.businessPartnerIdentityRepository.flush();
        var bpi = repos.businessPartnerIdentityRepository.save(newDefaultBusinessPartnerIdentity());
        TestTransaction.flagForCommit();
        TestTransaction.end();

        var request = createProtectedVerificationAuthorizationRequest(bpi.getId());

        // When
        var resultActions = mockMvc.perform(
            MockMvcRequestBuilders.post("/ui-api/v1/protected-verification-authorization/")
                .content(RequestTestData.objectMapper.writeValueAsString(request))
                .contentType(MediaType.APPLICATION_JSON)
        );

        // Then
        resultActions
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.id").isNotEmpty())
            .andExpect(jsonPath("$.businessPartnerIdentityId").value(bpi.getId().toString()))
            .andExpect(jsonPath("$.authorizableField").value("AHV_NUMBER"));

        verifyBusinessPartnerIdentityUpdateEventIsEmitted(bpi.getId());
    }

    @Test
    @WithJeapAuthenticationToken(userRoles = { EDITOR })
    void deleteProtectedVerificationAuthorization() throws Exception {
        // Given
        var bpi = repos.businessPartnerIdentityRepository.save(newDefaultBusinessPartnerIdentity());
        var pva = repos.protectedVerificationRepository.save(
            ProtectedVerificationAuthorizationTestData.defaultProtectedVerificationAuthorization(bpi.getId())
        );

        // When
        var resultActions = mockMvc.perform(
            MockMvcRequestBuilders.delete("/ui-api/v1/protected-verification-authorization/" + pva.getId())
        );

        // Then
        resultActions.andExpect(status().isNoContent());

        await().untilAsserted(() -> assertThat(repos.protectedVerificationRepository.findAll()).isEmpty());

        verifyBusinessPartnerIdentityUpdateEventIsEmitted(bpi.getId());
    }
}
