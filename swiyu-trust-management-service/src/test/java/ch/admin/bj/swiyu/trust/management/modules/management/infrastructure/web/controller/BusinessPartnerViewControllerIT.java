package ch.admin.bj.swiyu.trust.management.modules.management.infrastructure.web.controller;

import static ch.admin.bj.swiyu.trust.management.modules.common.auth.UserRole.Names.READER;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import ch.admin.bit.jeap.security.test.WithJeapAuthenticationToken;
import ch.admin.bj.swiyu.trust.management.modules.management.domain.BusinessPartner;
import ch.admin.bj.swiyu.trust.management.modules.management.domain.BusinessPartnerIdentity;
import ch.admin.bj.swiyu.trust.management.modules.management.domain.BusinessPartnerIdentityRepository;
import ch.admin.bj.swiyu.trust.management.modules.management.domain.BusinessPartnerIdentityStatus;
import ch.admin.bj.swiyu.trust.management.modules.management.domain.BusinessPartnerRepository;
import ch.admin.bj.swiyu.trust.management.modules.management.domain.BusinessPartnerType;
import ch.admin.bj.swiyu.trust.management.test.MockAuditPublisherTestConfiguration;
import ch.admin.bj.swiyu.trust.management.test.PostgreSQLContainerInitializer;
import ch.admin.bj.swiyu.trust.management.test.StatusListServiceTestConfiguration;
import java.time.Instant;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
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
class BusinessPartnerViewControllerIT {

    private static final String ENDPOINT = "/ui-api/v1/business-partner-view/";
    private static final UUID PARTNER_ID = UUID.fromString("44444444-4444-4444-4444-444444444444");
    private static final String PARTNER_NAME = "Zeta Partner View Test AG";

    @Autowired
    private BusinessPartnerRepository businessPartnerRepository;

    @Autowired
    private BusinessPartnerIdentityRepository businessPartnerIdentityRepository;

    @Autowired
    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        var partner = BusinessPartner.createFor(PARTNER_ID);
        partner.sync(PARTNER_NAME, "CHE-123.456.789", BusinessPartnerType.BUSINESS, true, Instant.now());
        businessPartnerRepository.save(partner);
        businessPartnerIdentityRepository.save(
            new BusinessPartnerIdentity(
                PARTNER_ID,
                Map.of("default", PARTNER_NAME),
                Instant.now(),
                "CHE-123.456.789",
                false,
                "de-CH",
                BusinessPartnerIdentityStatus.ACTIVE,
                false,
                Instant.parse("2029-03-31T15:00:00Z"),
                null,
                Set.of("did:example:partner-view")
            )
        );
    }

    @AfterEach
    void tearDown() {
        // Not transactional, so nothing rolls these rows back and the shared database container would leak them.
        businessPartnerIdentityRepository.findById(PARTNER_ID).ifPresent(businessPartnerIdentityRepository::delete);
        businessPartnerRepository.findById(PARTNER_ID).ifPresent(businessPartnerRepository::delete);
    }

    @Test
    @WithJeapAuthenticationToken(userRoles = { READER })
    void getBusinessPartnerViews_returnsThePartnerMatchingTheSearchText() throws Exception {
        mockMvc
            .perform(MockMvcRequestBuilders.get(ENDPOINT).param("searchText", "zeta partner view"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.content.length()").value(1))
            .andExpect(jsonPath("$.content[0].businessPartnerId").value(PARTNER_ID.toString()))
            .andExpect(jsonPath("$.content[0].name").value(PARTNER_NAME))
            .andExpect(jsonPath("$.content[0].status").value("ACTIVE"))
            .andExpect(jsonPath("$.content[0].validUntil").isNotEmpty())
            .andExpect(jsonPath("$.page.totalElements").value(1));
    }

    @Test
    @WithJeapAuthenticationToken(userRoles = { READER })
    void getBusinessPartnerViews_findsThePartnerByItsId() throws Exception {
        mockMvc
            .perform(MockMvcRequestBuilders.get(ENDPOINT).param("searchText", PARTNER_ID.toString()))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.content[0].businessPartnerId").value(PARTNER_ID.toString()));
    }

    @Test
    @WithJeapAuthenticationToken(userRoles = {})
    void getBusinessPartnerViews_isRejectedForATokenWithoutEditorOrReaderRole() throws Exception {
        mockMvc.perform(MockMvcRequestBuilders.get(ENDPOINT)).andExpect(status().isUnauthorized());
    }
}
