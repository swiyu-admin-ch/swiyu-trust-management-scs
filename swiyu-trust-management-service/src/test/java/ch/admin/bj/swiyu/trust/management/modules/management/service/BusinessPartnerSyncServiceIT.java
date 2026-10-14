package ch.admin.bj.swiyu.trust.management.modules.management.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

import ch.admin.bj.swiyu.trust.client.core.business.internal.api.BusinessPartnerV2Api;
import ch.admin.bj.swiyu.trust.client.core.business.internal.model.BusinessPartnerDto;
import ch.admin.bj.swiyu.trust.client.core.business.internal.model.BusinessPartnerTypeDto;
import ch.admin.bj.swiyu.trust.management.modules.common.exception.ExternalSystemException;
import ch.admin.bj.swiyu.trust.management.modules.management.domain.BusinessPartnerType;
import ch.admin.bj.swiyu.trust.management.test.DataJpaTestConfiguration;
import ch.admin.bj.swiyu.trust.management.test.PostgreSQLContainerInitializer;
import ch.admin.bj.swiyu.trust.management.test.TestRepositories;
import java.time.Instant;
import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpStatus;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.HttpServerErrorException;
import org.testcontainers.junit.jupiter.Testcontainers;

@Testcontainers
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@ContextConfiguration(initializers = PostgreSQLContainerInitializer.class)
@DataJpaTest
@Import({ BusinessPartnerSyncService.class, DataJpaTestConfiguration.class })
@ActiveProfiles("test")
class BusinessPartnerSyncServiceIT {

    private static final UUID PARTNER_ID = UUID.randomUUID();
    private static final String DEFAULT_KEY = "default";
    private static final String NAME = "Acme AG";

    @Autowired
    private BusinessPartnerSyncService service;

    @Autowired
    private TestRepositories repos;

    @MockitoBean
    private BusinessPartnerV2Api businessPartnerApi;

    @Test
    void syncPartner_createsTheProjectionWhenItDoesNotExistYet() {
        when(businessPartnerApi.getBusinessPartner(PARTNER_ID)).thenReturn(dto().hardDeleteAllowed(true));

        service.syncPartner(PARTNER_ID);

        var partner = repos.businessPartner.findById(PARTNER_ID).orElseThrow();
        assertThat(partner.getId()).isEqualTo(PARTNER_ID);
        assertThat(partner.getUid()).isEqualTo("CHE-123.456.789");
        assertThat(partner.getType()).isEqualTo(BusinessPartnerType.BUSINESS);
        assertThat(partner.isHardDeleteAllowed()).isTrue();
        assertThat(partner.getSyncedAt()).isNotNull();
    }

    @Test
    void syncPartner_flattensTheLocalizedNameToItsDefaultEntry() {
        when(businessPartnerApi.getBusinessPartner(PARTNER_ID)).thenReturn(
            dto().entityName(Map.of(DEFAULT_KEY, NAME, "de-CH", "Acme AG (DE)", "fr-CH", "Acme SA"))
        );

        service.syncPartner(PARTNER_ID);

        assertThat(repos.businessPartner.findById(PARTNER_ID).orElseThrow().getName()).isEqualTo(NAME);
    }

    @Test
    void syncPartner_updatesTheExistingProjectionInsteadOfAddingASecondOne() {
        when(businessPartnerApi.getBusinessPartner(PARTNER_ID)).thenReturn(
            dto().entityName(Map.of(DEFAULT_KEY, "Before")).hardDeleteAllowed(false)
        );
        service.syncPartner(PARTNER_ID);
        var firstSyncedAt = repos.businessPartner.findById(PARTNER_ID).orElseThrow().getSyncedAt();

        when(businessPartnerApi.getBusinessPartner(PARTNER_ID)).thenReturn(
            dto().entityName(Map.of(DEFAULT_KEY, "After")).hardDeleteAllowed(true)
        );
        service.syncPartner(PARTNER_ID);

        assertThat(repos.businessPartner.count()).isEqualTo(1);
        var partner = repos.businessPartner.findById(PARTNER_ID).orElseThrow();
        assertThat(partner.getName()).isEqualTo("After");
        assertThat(partner.isHardDeleteAllowed()).isTrue();
        assertThat(partner.getSyncedAt()).isAfterOrEqualTo(firstSyncedAt);
    }

    @Test
    void syncPartner_removesTheProjectionWhenCbsNoLongerKnowsThePartner() {
        when(businessPartnerApi.getBusinessPartner(PARTNER_ID)).thenReturn(dto());
        service.syncPartner(PARTNER_ID);
        assertThat(repos.businessPartner.findById(PARTNER_ID)).isPresent();

        when(businessPartnerApi.getBusinessPartner(PARTNER_ID)).thenThrow(
            new HttpClientErrorException(HttpStatus.NOT_FOUND)
        );
        service.syncPartner(PARTNER_ID);

        assertThat(repos.businessPartner.findById(PARTNER_ID)).isEmpty();
    }

    @Test
    void syncPartner_reportsAnUnreachableCoreBusinessServiceAsAnExternalSystemFailure() {
        when(businessPartnerApi.getBusinessPartner(PARTNER_ID)).thenThrow(
            new HttpServerErrorException(HttpStatus.SERVICE_UNAVAILABLE)
        );

        assertThatThrownBy(() -> service.syncPartner(PARTNER_ID))
            .isInstanceOf(ExternalSystemException.class)
            .hasMessageContaining("CORE_BUSINESS_SERVICE");

        assertThat(repos.businessPartner.findById(PARTNER_ID)).isEmpty();
    }

    private static BusinessPartnerDto dto() {
        return new BusinessPartnerDto()
            .id(PARTNER_ID)
            .entityName(Map.of(DEFAULT_KEY, NAME))
            .uid("CHE-123.456.789")
            .type(BusinessPartnerTypeDto.BUSINESS)
            .createdAt(Instant.now())
            .updatedAt(Instant.now());
    }
}
