package ch.admin.bj.swiyu.trust.management.modules.ui.infrastructure.web.controller;

import static ch.admin.bj.swiyu.trust.management.modules.common.auth.UserRole.Names.EDITOR;
import static ch.admin.bj.swiyu.trust.management.modules.common.auth.UserRole.Names.READER;
import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.not;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import ch.admin.bit.jeap.security.test.WithJeapAuthenticationToken;
import ch.admin.bj.swiyu.trust.management.modules.management.api.task.CreateDeleteBusinessPartnerTaskRequestDto;
import ch.admin.bj.swiyu.trust.management.modules.management.domain.BusinessPartner;
import ch.admin.bj.swiyu.trust.management.modules.management.domain.BusinessPartnerRepository;
import ch.admin.bj.swiyu.trust.management.modules.management.domain.BusinessPartnerType;
import ch.admin.bj.swiyu.trust.management.modules.management.service.DeleteBusinessPartnerTaskService;
import ch.admin.bj.swiyu.trust.management.test.MockAuditPublisherTestConfiguration;
import ch.admin.bj.swiyu.trust.management.test.PostgreSQLContainerInitializer;
import ch.admin.bj.swiyu.trust.management.test.StatusListServiceTestConfiguration;
import java.time.Instant;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;
import org.testcontainers.junit.jupiter.Testcontainers;

@ActiveProfiles("test")
@SpringBootTest(properties = { "kafka.enable=false", "app.offboarding.hard-delete-delay=5m" })
@AutoConfigureMockMvc
@Testcontainers
@ContextConfiguration(initializers = PostgreSQLContainerInitializer.class)
@Import({ StatusListServiceTestConfiguration.class, MockAuditPublisherTestConfiguration.class })
@Transactional
class DeleteBusinessPartnerTaskControllerIT {

    private static final String CREATE_ENDPOINT = "/ui-api/tasks/delete-business-partner";
    private static final String DETAIL_ENDPOINT = "/ui-api/tasks/{id}/delete-business-partner";
    private static final String REJECT_ENDPOINT = "/ui-api/tasks/{id}/delete-business-partner/reject";

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private BusinessPartnerRepository partners;

    @Autowired
    private DeleteBusinessPartnerTaskService service;

    @Test
    @WithJeapAuthenticationToken(userRoles = { READER })
    void configurationExposesTheFeatureToggleAndTheConfiguredDelayInSeconds() throws Exception {
        mockMvc
            .perform(get("/ui-api/configuration/"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.featureToggles.EIDARTFE_1571_COMPLETE_BP_OFFBOARDING").value(true))
            .andExpect(jsonPath("$.hardDeleteDelaySeconds").value(300));
    }

    @Test
    @WithJeapAuthenticationToken(userRoles = { EDITOR })
    void canCreateReadAndRejectButCannotApproveDeletion() throws Exception {
        var partnerId = partner();
        var response = mockMvc
            .perform(
                post(CREATE_ENDPOINT)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(request(partnerId, "A reason", true))
            )
            .andExpect(status().isCreated())
            .andReturn();
        var taskId = UUID.fromString(response.getResponse().getContentAsString().replace("\"", ""));
        var deadline = service.getTask(taskId, "tester").hardDeleteDueAt().toString();
        mockMvc
            .perform(get("/ui-api/tasks/").param("taskType", "DELETE_BUSINESS_PARTNER"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.content[*].id", hasItem(taskId.toString())));
        mockMvc
            .perform(get(DETAIL_ENDPOINT, taskId))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.hardDeleteDueAt").value(deadline))
            .andExpect(jsonPath("$.partnerName").value("Deletion controller test"))
            .andExpect(jsonPath("$.taskType").value("DELETE_BUSINESS_PARTNER"))
            .andExpect(jsonPath("$.allowedActions", hasItem("REJECT")))
            .andExpect(jsonPath("$.allowedActions", not(hasItem("APPROVE"))));
        mockMvc
            .perform(
                post(REJECT_ENDPOINT, taskId)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content("{\"internalNote\":\"Keep the partner\"}")
            )
            .andExpect(status().isOk());
        mockMvc
            .perform(get(DETAIL_ENDPOINT, taskId))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.state").value("REJECTED"))
            .andExpect(jsonPath("$.hardDeleteDueAt").value(deadline))
            .andExpect(jsonPath("$.allowedActions", not(hasItem("REJECT"))));
    }

    @Test
    @WithJeapAuthenticationToken(userRoles = { READER })
    void readerCannotCreateOrReject() throws Exception {
        var partnerId = partner();
        var taskId = service.createTask(
            new CreateDeleteBusinessPartnerTaskRequestDto(partnerId, "reason", true),
            "tester"
        );
        mockMvc
            .perform(
                post(CREATE_ENDPOINT)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(request(partnerId, "reason", true))
            )
            .andExpect(status().isUnauthorized());
        mockMvc
            .perform(
                post(REJECT_ENDPOINT, taskId)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content("{\"internalNote\":\"Keep it\"}")
            )
            .andExpect(status().isUnauthorized());
    }

    @Test
    @WithJeapAuthenticationToken(userRoles = { EDITOR })
    void requiresReasonAndIrreversibilityConfirmation() throws Exception {
        var partnerId = partner();
        mockMvc
            .perform(
                post(CREATE_ENDPOINT).contentType(MediaType.APPLICATION_JSON).content(request(partnerId, " ", true))
            )
            .andExpect(status().isBadRequest());
        mockMvc
            .perform(
                post(CREATE_ENDPOINT)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(request(partnerId, "reason", false))
            )
            .andExpect(status().isBadRequest());
    }

    @Test
    @WithJeapAuthenticationToken(userRoles = { EDITOR })
    void requiresInternalNoteToReject() throws Exception {
        var taskId = service.createTask(
            new CreateDeleteBusinessPartnerTaskRequestDto(partner(), "reason", true),
            "tester"
        );
        mockMvc
            .perform(
                post(REJECT_ENDPOINT, taskId)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content("{\"internalNote\":\" \"}")
            )
            .andExpect(status().isBadRequest());
    }

    private UUID partner() {
        var id = UUID.randomUUID();
        var partner = BusinessPartner.createFor(id);
        partner.sync("Deletion controller test", "CHE-123.456.789", BusinessPartnerType.BUSINESS, true, Instant.now());
        partners.save(partner);
        return id;
    }

    private String request(UUID partnerId, String reason, boolean confirmed) {
        return "{\"businessPartnerId\":\"%s\",\"reason\":\"%s\",\"irreversibleDeletionConfirmed\":%s}".formatted(
            partnerId,
            reason,
            confirmed
        );
    }
}
