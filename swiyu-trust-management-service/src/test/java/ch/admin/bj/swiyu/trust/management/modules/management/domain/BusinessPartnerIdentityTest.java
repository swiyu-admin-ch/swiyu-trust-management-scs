package ch.admin.bj.swiyu.trust.management.modules.management.domain;

import static ch.admin.bj.swiyu.trust.management.test.BusinessPartnerIdentityTestData.businessPartnerIdentityExpiringIn30Days;
import static org.assertj.core.api.Assertions.assertThat;

import java.util.Map;
import java.util.Set;
import org.junit.jupiter.api.Test;

class BusinessPartnerIdentityTest {

    @Test
    void applyRelevantInformationUpdate_isRegisteredInCommercialRegisterNull_keepsCurrentValue() {
        // Given - an identity of a partner that is registered in the commercial register
        var bpi = businessPartnerIdentityExpiringIn30Days();
        assertThat(bpi.getIsRegisteredInCommercialRegister()).isTrue();

        // When - a re-verification arrives, which does not carry the flag
        bpi.applyRelevantInformationUpdate(update(null));

        // Then
        assertThat(bpi.getIsRegisteredInCommercialRegister()).isTrue();
    }

    @Test
    void applyRelevantInformationUpdate_isRegisteredInCommercialRegisterSet_overwritesCurrentValue() {
        // Given
        var bpi = businessPartnerIdentityExpiringIn30Days();

        // When - a registration carries the flag and is authoritative
        bpi.applyRelevantInformationUpdate(update(false));

        // Then
        assertThat(bpi.getIsRegisteredInCommercialRegister()).isFalse();
    }

    @Test
    void applyRelevantInformationUpdate_replacesTrustedIdentifiers() {
        // Given
        var bpi = businessPartnerIdentityExpiringIn30Days();
        assertThat(bpi.getTrustedIdentifier()).containsExactlyInAnyOrder("did1", "did2");

        // When
        bpi.applyRelevantInformationUpdate(update(null));

        // Then
        assertThat(bpi.getTrustedIdentifier()).containsExactly("did:proven");
    }

    private static BusinessPartnerIdentityRelevantInformationUpdate update(Boolean isRegisteredInCommercialRegister) {
        return new BusinessPartnerIdentityRelevantInformationUpdate(
            Map.of("default", "Test Partner"),
            "CHE-123-456-789",
            isRegisteredInCommercialRegister,
            "de-CH",
            false,
            Set.of("did:proven")
        );
    }
}
