package ch.admin.bj.swiyu.trust.management.modules.management.service;

import static org.assertj.core.api.Assertions.assertThat;

import ch.admin.bj.swiyu.trust.management.modules.management.api.BusinessPartnerIdentityStatusDto;
import ch.admin.bj.swiyu.trust.management.modules.management.api.BusinessPartnerViewFilterDto;
import ch.admin.bj.swiyu.trust.management.modules.management.api.BusinessPartnerViewListItemDto;
import ch.admin.bj.swiyu.trust.management.modules.management.domain.BusinessPartner;
import ch.admin.bj.swiyu.trust.management.modules.management.domain.BusinessPartnerIdentity;
import ch.admin.bj.swiyu.trust.management.modules.management.domain.BusinessPartnerIdentityStatus;
import ch.admin.bj.swiyu.trust.management.modules.management.domain.BusinessPartnerType;
import ch.admin.bj.swiyu.trust.management.test.DataJpaTestConfiguration;
import ch.admin.bj.swiyu.trust.management.test.PostgreSQLContainerInitializer;
import ch.admin.bj.swiyu.trust.management.test.TestRepositories;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase;
import org.springframework.context.annotation.Import;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.ContextConfiguration;
import org.testcontainers.junit.jupiter.Testcontainers;

@Testcontainers
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@ContextConfiguration(initializers = PostgreSQLContainerInitializer.class)
@DataJpaTest
@Import({ BusinessPartnerViewService.class, DataJpaTestConfiguration.class })
@ActiveProfiles("test")
class BusinessPartnerViewServiceIT {

    private static final UUID SYNCED_AND_ONBOARDED = UUID.fromString("11111111-1111-1111-1111-111111111111");
    private static final UUID ONLY_SYNCED = UUID.fromString("22222222-2222-2222-2222-222222222222");
    private static final UUID ONLY_ONBOARDED = UUID.fromString("33333333-3333-3333-3333-333333333333");

    /** Narrows every assertion to the partners of this test, since the database container is shared. */
    private static final String SCOPE = "ViewTest";

    private static final String ACME = "Acme AG";
    private static final String BERNESE_BANK = "Bernese Bank AG";
    private static final String COMTEL = "Comtel GmbH";

    private static final String DEFAULT_KEY = "default";
    private static final Instant VALID_UNTIL = Instant.parse("2029-03-31T15:00:00Z");
    private static final Pageable FIRST_PAGE_BY_NAME = PageRequest.of(0, 10, Sort.by("partnerName").ascending());

    @Autowired
    private BusinessPartnerViewService service;

    @Autowired
    private TestRepositories repos;

    @BeforeEach
    void setUp() {
        repos.businessPartner.saveAndFlush(projection(SYNCED_AND_ONBOARDED, ACME));
        repos.businessPartnerIdentity.saveAndFlush(
            identity(SYNCED_AND_ONBOARDED, ACME, BusinessPartnerIdentityStatus.ACTIVE)
        );
        repos.businessPartner.saveAndFlush(projection(ONLY_SYNCED, BERNESE_BANK));
        repos.businessPartnerIdentity.saveAndFlush(
            identity(ONLY_ONBOARDED, COMTEL, BusinessPartnerIdentityStatus.DEACTIVATED)
        );
    }

    @Test
    void getBusinessPartnerViews_joinsTheIdentityOntoTheProjection() {
        var partner = partnerOfTest(SYNCED_AND_ONBOARDED);

        assertThat(partner).isPresent();
        assertThat(partner.get().name()).isEqualTo(scoped(ACME));
        assertThat(partner.get().status()).isEqualTo(BusinessPartnerIdentityStatusDto.ACTIVE);
        assertThat(partner.get().validUntil()).isEqualTo(VALID_UNTIL);
    }

    @Test
    void getBusinessPartnerViews_listsAPartnerThatHasNeverBeenOnboarded_withoutAStatus() {
        var partner = partnerOfTest(ONLY_SYNCED);

        assertThat(partner).isPresent();
        assertThat(partner.get().name()).isEqualTo(scoped(BERNESE_BANK));
        assertThat(partner.get().status()).isNull();
        assertThat(partner.get().validUntil()).isNull();
    }

    @Test
    void getBusinessPartnerViews_listsAPartnerThatHasNotBeenSyncedYet_withTheNameOfItsIdentity() {
        var partner = partnerOfTest(ONLY_ONBOARDED);

        assertThat(partner).isPresent();
        assertThat(partner.get().name()).isEqualTo(scoped(COMTEL));
        assertThat(partner.get().status()).isEqualTo(BusinessPartnerIdentityStatusDto.DEACTIVATED);
    }

    @Test
    void getBusinessPartnerViews_sortsByNameAndPages() {
        var firstPage = service.getBusinessPartnerViews(
            new BusinessPartnerViewFilterDto(SCOPE),
            PageRequest.of(0, 2, Sort.by("partnerName").ascending())
        );

        assertThat(firstPage.getTotalElements()).isEqualTo(3);
        assertThat(firstPage.getContent())
            .extracting(BusinessPartnerViewListItemDto::name)
            .containsExactly(scoped(ACME), scoped(BERNESE_BANK));
    }

    @Test
    void getBusinessPartnerViews_searchesPartOfTheNameIgnoringCase() {
        var found = search("viewtest bernese");

        assertThat(found).extracting(BusinessPartnerViewListItemDto::businessPartnerId).containsExactly(ONLY_SYNCED);
    }

    @Test
    void getBusinessPartnerViews_searchesPartOfThePartnerId() {
        var found = search("33333333-3333");

        assertThat(found).extracting(BusinessPartnerViewListItemDto::businessPartnerId).containsExactly(ONLY_ONBOARDED);
    }

    @Test
    void getBusinessPartnerViews_returnsNothingWhenTheSearchMatchesNeitherNameNorId() {
        var found = search("no such partner");

        assertThat(found).isEmpty();
    }

    @Test
    void getBusinessPartnerViews_ignoresABlankSearchText() {
        var withBlankSearch = search("   ");
        var withoutSearch = search(null);

        assertThat(withBlankSearch).isEqualTo(withoutSearch);
        assertThat(withBlankSearch).extracting(BusinessPartnerViewListItemDto::businessPartnerId).isNotEmpty();
    }

    private Optional<BusinessPartnerViewListItemDto> partnerOfTest(UUID businessPartnerId) {
        return search(SCOPE)
            .stream()
            .filter(item -> businessPartnerId.equals(item.businessPartnerId()))
            .findFirst();
    }

    private List<BusinessPartnerViewListItemDto> search(String searchText) {
        return service
            .getBusinessPartnerViews(new BusinessPartnerViewFilterDto(searchText), FIRST_PAGE_BY_NAME)
            .getContent();
    }

    private static String scoped(String name) {
        return SCOPE + " " + name;
    }

    private static BusinessPartner projection(UUID id, String name) {
        var partner = BusinessPartner.createFor(id);
        partner.sync(scoped(name), "CHE-123.456.789", BusinessPartnerType.BUSINESS, true, Instant.now());
        return partner;
    }

    private static BusinessPartnerIdentity identity(UUID id, String name, BusinessPartnerIdentityStatus status) {
        return new BusinessPartnerIdentity(
            id,
            Map.of(DEFAULT_KEY, scoped(name), "de-CH", scoped(name)),
            Instant.now(),
            "CHE-123.456.789",
            false,
            "de-CH",
            status,
            false,
            VALID_UNTIL,
            null,
            Set.of("did:example:" + id)
        );
    }
}
