package ch.admin.bj.swiyu.trust.management.modules.management.service;

import static org.assertj.core.api.Assertions.assertThat;

import ch.admin.bj.swiyu.trust.management.modules.management.domain.details.IdentityV1Details;
import ch.admin.bj.swiyu.trust.management.modules.management.domain.details.IdentityV2Details;
import ch.admin.bj.swiyu.trust.management.modules.management.domain.details.ProtectedIssuanceAuthorizationV2Details;
import ch.admin.bj.swiyu.trust.management.modules.management.domain.details.VerificationQueryV2Details;
import java.util.Map;
import org.junit.jupiter.api.Test;

class TrustStatementMapperTest {

    @Test
    @SuppressWarnings("java:S5738") // EID-6303
    void toIdentityV1LanguageMap_mapsDefaultKeyToDefaultLanguage() {
        var source = Map.of("default", "val_default", "de-CH", "val_de", "en", "val_en");

        var result = TrustStatementMapper.toIdentityV1LanguageMap(source);

        assertThat(result)
            .containsEntry(IdentityV1Details.Language.DEFAULT, "val_default")
            .containsEntry(IdentityV1Details.Language.DE_CH, "val_de")
            .containsEntry(IdentityV1Details.Language.EN, "val_en");
    }

    @Test
    @SuppressWarnings("java:S5738") // EID-6303
    void toProtectedIssuanceAuthorizationV2DetailsLanguageMap_mapsDefaultKeyToDefaultLanguage() {
        var source = Map.of("de-CH", "val_de", "default", "val_default", "en", "val_en");

        var result = TrustStatementMapper.toProtectedIssuanceAuthorizationV2DetailsLanguageMap(source);

        assertThat(result)
            .containsEntry(ProtectedIssuanceAuthorizationV2Details.Language.DEFAULT, "val_default")
            .containsEntry(ProtectedIssuanceAuthorizationV2Details.Language.DE_CH, "val_de")
            .containsEntry(ProtectedIssuanceAuthorizationV2Details.Language.EN, "val_en");
    }

    @Test
    @SuppressWarnings("java:S5738") // EID-6303
    void toVerificationQueryV2LanguageMap_mapsDefaultKeyToDefaultLanguage() {
        var source = Map.of("default", "val_default", "de-CH", "val_de", "en", "val_en");

        var result = TrustStatementMapper.toVerificationQueryV2LanguageMap(source);

        assertThat(result)
            .containsEntry(VerificationQueryV2Details.Language.DEFAULT, "val_default")
            .containsEntry(VerificationQueryV2Details.Language.DE_CH, "val_de")
            .containsEntry(VerificationQueryV2Details.Language.EN, "val_en");
    }

    @Test
    @SuppressWarnings("java:S5738") // EID-6303
    void toIdentityV2LanguageMap_mapsDefaultKeyToDefaultLanguage() {
        var source = Map.of("default", "val_default", "de-CH", "val_de", "en", "val_en");

        var result = TrustStatementMapper.toIdentityV2LanguageMap(source);

        assertThat(result)
            .containsEntry(IdentityV2Details.Language.DEFAULT, "val_default")
            .containsEntry(IdentityV2Details.Language.DE_CH, "val_de")
            .containsEntry(IdentityV2Details.Language.EN, "val_en");
    }
}
