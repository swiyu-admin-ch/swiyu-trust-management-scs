package ch.admin.bj.swiyu.trust.management.modules.dataimport.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.within;

import ch.admin.bj.swiyu.trust.management.modules.dataimport.domain.DemoData;
import ch.admin.bj.swiyu.trust.management.modules.management.domain.BusinessPartnerIdentityStatus;
import java.time.Duration;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Arrays;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

/**
 * The demo partners are the only way the re-verification flows can be demoed on a stage, so their expiry date has to
 * be the one the demo data asks for.
 */
class DemoDataMapperTest {

    @ParameterizedTest
    @EnumSource(DemoData.DemoCase.class)
    void toBusinessPartnerIdentity_takesTheExpiryFromTheDemoData(DemoData.DemoCase demoCase) {
        if (demoCase.bp.bpi() == null) {
            return; // not every demo case has an identity
        }

        var identity = DemoDataMapper.toBusinessPartnerIdentity(demoCase.bp);

        assertThat(identity.getValidUntil())
            .as("the configured expiry of %s is what the identity carries", demoCase.name())
            .isEqualTo(demoCase.bp.bpi().validUntil());
    }

    /**
     * The two cases that drive the re-verification demos: both configure an identity that runs out in 30 days, which is
     * what puts them inside the window where a renewal or a mandatory profile change is offered.
     */
    @Test
    void toBusinessPartnerIdentity_reVerificationCases_expireIn30Days() {
        var reVerificationCases = Arrays.stream(DemoData.DemoCase.values())
            .filter(demoCase -> demoCase.name().startsWith("BP_RE_VERIFICATION_REQUIRED"))
            .toList();

        assertThat(reVerificationCases).as("the demo data still carries the re-verification cases").hasSize(2);

        for (var demoCase : reVerificationCases) {
            var identity = DemoDataMapper.toBusinessPartnerIdentity(demoCase.bp);

            assertThat(identity.getStatus()).isEqualTo(BusinessPartnerIdentityStatus.ACTIVE);
            assertThat(identity.getValidUntil())
                .as("%s expires in 30 days instead of nearly three years", demoCase.name())
                .isCloseTo(Instant.now().plus(30, ChronoUnit.DAYS), within(1, ChronoUnit.MINUTES));
            assertThat(Duration.between(Instant.now(), identity.getValidUntil()))
                .as("%s sits well inside the renewal window", demoCase.name())
                .isLessThan(Duration.ofDays(90));
        }
    }
}
