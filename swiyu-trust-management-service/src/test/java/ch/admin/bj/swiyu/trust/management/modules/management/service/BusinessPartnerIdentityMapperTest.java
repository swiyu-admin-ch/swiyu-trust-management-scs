package ch.admin.bj.swiyu.trust.management.modules.management.service;

import static org.assertj.core.api.Assertions.assertThat;

import ch.admin.bj.swiyu.trust.management.modules.management.domain.details.IdentityV1Details;
import ch.admin.bj.swiyu.trust.management.test.TrustOnboardingTestData;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;

class BusinessPartnerIdentityMapperTest {

    @Test
    void toBusinessPartnerIdentity_onboarding_preservesCbsDefaultEntry() {
        var submission = TrustOnboardingTestData.trustOnboardingSubmissionDto();

        var identity = BusinessPartnerIdentityMapper.toBusinessPartnerIdentity(submission);

        assertThat(identity.entityName()).isEqualTo(submission.getName());
        assertThat(identity.entityName()).containsEntry("default", "Migros");
    }

    @Test
    @SuppressWarnings("java:S5738") // EID-6303
    void toLocalizedEntityName_addDid_roundTripsStoredDefaultLanguage() {
        var details = new IdentityV1Details(
            Map.of(
                IdentityV1Details.Language.DEFAULT,
                "Acme",
                IdentityV1Details.Language.DE_CH,
                "Acme DE",
                IdentityV1Details.Language.EN,
                "Acme EN"
            ),
            false,
            List.of()
        );

        var entityName = BusinessPartnerIdentityMapper.toLocalizedEntityName(details);

        // The explicitly stored DEFAULT wins; no de-first guessing.
        assertThat(entityName)
            .containsEntry("default", "Acme")
            .containsEntry("de-CH", "Acme DE")
            .containsEntry("en", "Acme EN");
    }

    @Test
    @SuppressWarnings("java:S5738") // EID-6303
    void toLocalizedEntityName_addDid_legacyStatementWithoutDefault_fallsBackToFirstLocale() {
        var details = new IdentityV1Details(
            Map.of(IdentityV1Details.Language.DE_CH, "Acme DE", IdentityV1Details.Language.EN, "Acme EN"),
            false,
            List.of()
        );

        var entityName = BusinessPartnerIdentityMapper.toLocalizedEntityName(details);

        // Legacy statements without a stored DEFAULT get one synthesized from the lexicographically first locale.
        assertThat(entityName)
            .containsEntry("default", "Acme DE")
            .containsEntry("de-CH", "Acme DE")
            .containsEntry("en", "Acme EN");
    }
}
