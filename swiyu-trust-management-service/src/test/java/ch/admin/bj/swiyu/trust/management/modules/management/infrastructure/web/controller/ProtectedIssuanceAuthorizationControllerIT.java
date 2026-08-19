package ch.admin.bj.swiyu.trust.management.modules.management.infrastructure.web.controller;

import static ch.admin.bj.swiyu.trust.management.modules.common.auth.UserRole.Names.EDITOR;
import static ch.admin.bj.swiyu.trust.management.modules.common.i18n.LocalizedMapUtil.fromLanguages;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import ch.admin.bit.jeap.security.test.WithJeapAuthenticationToken;
import ch.admin.bj.swiyu.messagetype.ti.TiBusinessPartnerIdentityUpdatedEvent;
import ch.admin.bj.swiyu.trust.management.modules.management.domain.*;
import ch.admin.bj.swiyu.trust.management.modules.management.domain.publisher.OutboxEventPublisher;
import ch.admin.bj.swiyu.trust.management.test.*;
import java.time.Instant;
import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;
import org.testcontainers.junit.jupiter.Testcontainers;
import tools.jackson.databind.ObjectMapper;

@ActiveProfiles("test")
@SpringBootTest(properties = { "kafka.enable=false" })
@AutoConfigureMockMvc
@Testcontainers
@ContextConfiguration(initializers = PostgreSQLContainerInitializer.class)
@Import({ StatusListServiceTestConfiguration.class, MockAuditPublisherTestConfiguration.class })
class ProtectedIssuanceAuthorizationControllerIT {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private BusinessPartnerIdentityRepository businessPartnerIdentityRepository;

    @Autowired
    private ProtectedIssuanceEntryRepository protectedIssuanceEntryRepository;

    @Autowired
    private ProtectedIssuanceAuthorizationRepository protectedIssuanceAuthorizationRepository;

    @Autowired
    private TrustStatementPartnerLinkRepository trustStatementPartnerLinkRepository;

    @Autowired
    private AsyncTestConfig asyncTestConfig;

    @MockitoBean
    private OutboxEventPublisher outboxEventPublisher;

    private BusinessPartnerIdentity savedBpi;
    private ProtectedIssuanceEntry savedEntry;

    @BeforeEach
    void setUp() {
        asyncTestConfig.waitForAsyncOperationsFinished();
        trustStatementPartnerLinkRepository.deleteAllInBatch();
        protectedIssuanceAuthorizationRepository.deleteAllInBatch();
        protectedIssuanceEntryRepository.deleteAllInBatch();
        businessPartnerIdentityRepository.deleteAllInBatch();

        savedBpi = businessPartnerIdentityRepository.saveAndFlush(
            BusinessPartnerIdentityTestData.newDefaultBusinessPartnerIdentity()
        );

        savedEntry = protectedIssuanceEntryRepository.saveAndFlush(
            new ProtectedIssuanceEntry(
                UUID.randomUUID(),
                "vct:example:protected:issuance",
                Instant.now(),
                fromLanguages(
                    "Test Protected issuance entry (Default)",
                    "Test Protected issuance entry (DE)",
                    "Test Protected issuance entry (FR)",
                    "Test Protected issuance entry (IT)",
                    "Test Protected issuance entry (EN)",
                    "Test Protected issuance entry (RM)"
                )
            )
        );
    }

    @Test
    @WithJeapAuthenticationToken(userRoles = { EDITOR })
    void addAuthorization_createsProtectedIssuanceAuthorizationAndPublishesUpdatedEvent() throws Exception {
        // Given
        var request = Map.of(
            "protectedIssuanceEntryId",
            savedEntry.getId().toString(),
            "businessPartnerIdentityId",
            savedBpi.getId().toString()
        );

        // When
        mockMvc
            .perform(
                MockMvcRequestBuilders.post("/ui-api/v1/protected-issuance-authorization/")
                    .content(objectMapper.writeValueAsString(request))
                    .contentType(MediaType.APPLICATION_JSON)
            )
            // Then
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.id").isNotEmpty())
            .andExpect(jsonPath("$.businessPartnerIdentityId").value(savedBpi.getId().toString()))
            .andExpect(jsonPath("$.protectedIssuanceEntryId").value(savedEntry.getId().toString()))
            .andExpect(jsonPath("$.vct").value("vct:example:protected:issuance"));

        assertThat(
            protectedIssuanceAuthorizationRepository.findAllByBusinessPartnerIdentityId(savedBpi.getId())
        ).hasSize(1);
        verify(outboxEventPublisher, times(1)).publishBusinessPartnerIdentityUpdatedEvent(
            any(TiBusinessPartnerIdentityUpdatedEvent.class)
        );
    }

    @Test
    @WithJeapAuthenticationToken(userRoles = { EDITOR })
    void getAuthorization_returnsCreatedProtectedIssuanceAuthorization() throws Exception {
        // Given — create authorization directly in repo
        var authorization = new ProtectedIssuanceAuthorization(
            UUID.randomUUID(),
            savedBpi.getId(),
            savedEntry.getId(),
            null
        );
        var saved = protectedIssuanceAuthorizationRepository.saveAndFlush(authorization);

        // When / Then
        mockMvc
            .perform(
                MockMvcRequestBuilders.get(
                    "/ui-api/v1/protected-issuance-authorization/{id}",
                    saved.getId()
                ).contentType(MediaType.APPLICATION_JSON)
            )
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.id").value(saved.getId().toString()))
            .andExpect(jsonPath("$.vct").value("vct:example:protected:issuance"))
            .andExpect(jsonPath("$.name['de-CH']").value("Test Protected issuance entry (DE)"));
    }

    @Test
    @WithJeapAuthenticationToken(userRoles = { EDITOR })
    void listProtectedIssuanceAuthorization_returnsAllForBpi() throws Exception {
        // Given — create authorization directly in repo
        var authorization = new ProtectedIssuanceAuthorization(
            UUID.randomUUID(),
            savedBpi.getId(),
            savedEntry.getId(),
            null
        );
        protectedIssuanceAuthorizationRepository.saveAndFlush(authorization);

        // When / Then
        mockMvc
            .perform(
                MockMvcRequestBuilders.get("/ui-api/v1/protected-issuance-authorization/")
                    .param("businessPartnerIdentityId", savedBpi.getId().toString())
                    .contentType(MediaType.APPLICATION_JSON)
            )
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.page.totalElements").value(1))
            .andExpect(jsonPath("$.content").isArray())
            .andExpect(jsonPath("$.content[0].businessPartnerIdentityId").value(savedBpi.getId().toString()))
            .andExpect(jsonPath("$.content[0].vct").value("vct:example:protected:issuance"));
    }

    @Test
    @WithJeapAuthenticationToken(userRoles = { EDITOR })
    void removeAuthorization_deletesProtectedIssuanceAuthorizationAndPublishesUpdatedEvent() throws Exception {
        // Given — create authorization directly in repo
        var authorization = new ProtectedIssuanceAuthorization(
            UUID.randomUUID(),
            savedBpi.getId(),
            savedEntry.getId(),
            null
        );
        var saved = protectedIssuanceAuthorizationRepository.saveAndFlush(authorization);

        // When
        mockMvc
            .perform(
                MockMvcRequestBuilders.delete(
                    "/ui-api/v1/protected-issuance-authorization/{id}",
                    saved.getId()
                ).contentType(MediaType.APPLICATION_JSON)
            )
            // Then
            .andExpect(status().isOk());

        assertThat(
            protectedIssuanceAuthorizationRepository.findAllByBusinessPartnerIdentityId(savedBpi.getId())
        ).isEmpty();
        verify(outboxEventPublisher, times(1)).publishBusinessPartnerIdentityUpdatedEvent(
            any(TiBusinessPartnerIdentityUpdatedEvent.class)
        );
    }
}
