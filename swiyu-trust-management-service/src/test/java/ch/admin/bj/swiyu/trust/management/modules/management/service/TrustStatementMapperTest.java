package ch.admin.bj.swiyu.trust.management.modules.management.service;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Map;
import org.junit.jupiter.api.Test;

class TrustStatementMapperTest {

    @Test
    void toIdentityV1LanguageMap_doesNotMapDefaultKey() {
        var source = Map.of("default", "val_default", "de-CH", "val_de", "en", "val_en");

        var result = TrustStatementMapper.toIdentityV1LanguageMap(source);

        assertThat(result).hasSize(2).containsEntry("de-CH", "val_de").containsEntry("en", "val_en");
    }

    @Test
    void toIdentityV1LanguageMap_mapsDefaultKeyOnlyIfNoOtherData() {
        var source = Map.of("default", "val_default");

        var result = TrustStatementMapper.toIdentityV1LanguageMap(source);

        assertThat(result).hasSize(1).containsEntry("de-CH", "val_default");
    }

    @Test
    void toProtectedIssuanceAuthorizationV2DetailsLanguageMap_mapsAnyLocaleStringAsIs() {
        var source = Map.of("de-CH", "val_de", "default", "val_default", "en", "val_en");

        var result = TrustStatementMapper.toProtectedIssuanceAuthorizationV2DetailsLanguageMap(source);

        assertThat(result)
            .containsEntry("default", "val_default")
            .containsEntry("de-CH", "val_de")
            .containsEntry("en", "val_en");
    }

    @Test
    void toVerificationQueryV2LanguageMap_mapsAnyLocaleStringAsIs() {
        var source = Map.of("default", "val_default", "de-CH", "val_de", "en", "val_en");

        var result = TrustStatementMapper.toVerificationQueryV2LanguageMap(source);

        assertThat(result)
            .containsEntry("default", "val_default")
            .containsEntry("de-CH", "val_de")
            .containsEntry("en", "val_en");
    }

    @Test
    void toVerificationQueryV2LanguageMap_keepsBareLocaleCodesAsIs() {
        var source = Map.of("default", "val_default", "fr", "val_fr", "it", "val_it", "rm", "val_rm");

        var result = TrustStatementMapper.toVerificationQueryV2LanguageMap(source);

        assertThat(result)
            .containsEntry("default", "val_default")
            .containsEntry("fr", "val_fr")
            .containsEntry("it", "val_it")
            .containsEntry("rm", "val_rm");
    }

    @Test
    void toIdentityV2LanguageMap_mapsAnyLocaleStringAsIs() {
        var source = Map.of("default", "val_default", "de-CH", "val_de", "en", "val_en");

        var result = TrustStatementMapper.toIdentityV2LanguageMap(source);

        assertThat(result)
            .containsEntry("default", "val_default")
            .containsEntry("de-CH", "val_de")
            .containsEntry("en", "val_en");
    }

    @Test
    void toIdentityV2LanguageMap_keepsBareLocaleCodeAsIs() {
        var source = Map.of("default", "val_default", "fr", "val_fr");

        var result = TrustStatementMapper.toIdentityV2LanguageMap(source);

        assertThat(result).containsEntry("default", "val_default").containsEntry("fr", "val_fr");
    }

    @Test
    void toProtectedIssuanceAuthorizationV2DetailsLanguageMap_keepsBareLocaleCodeAsIs() {
        var source = Map.of("default", "val_default", "fr", "val_fr");

        var result = TrustStatementMapper.toProtectedIssuanceAuthorizationV2DetailsLanguageMap(source);

        assertThat(result).containsEntry("default", "val_default").containsEntry("fr", "val_fr");
    }
}
