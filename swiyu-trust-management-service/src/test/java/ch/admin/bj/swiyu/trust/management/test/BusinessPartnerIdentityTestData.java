package ch.admin.bj.swiyu.trust.management.test;

import ch.admin.bj.swiyu.trust.management.modules.common.date.DateTimeHelper;
import ch.admin.bj.swiyu.trust.management.modules.management.domain.BusinessPartnerIdentity;
import ch.admin.bj.swiyu.trust.management.modules.management.domain.BusinessPartnerIdentityStatus;
import ch.admin.bj.swiyu.trust.management.modules.management.domain.TrustStatementPartnerLink;
import ch.admin.bj.swiyu.trust.management.modules.management.domain.TrustStatementPartnerLinkStatus;
import java.time.Instant;
import java.util.Collections;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import lombok.experimental.UtilityClass;

@UtilityClass
public class BusinessPartnerIdentityTestData {

    public static final Map<String, String> BUSINESS_PARTNER_NAME = Map.of(
        "default",
        "Test Partner",
        "de-CH",
        "Test Partner",
        "fr-CH",
        "Test Partner FR",
        "it-CH",
        "Test Partner IT",
        "en",
        "Test Partner EN",
        "rm-CH",
        "Test Partner RM"
    );

    public static BusinessPartnerIdentity newDefaultBusinessPartnerIdentity() {
        return newDefaultBusinessPartnerIdentity(BusinessPartnerIdentityStatus.ACTIVE);
    }

    public static BusinessPartnerIdentity newDefaultBusinessPartnerIdentity(BusinessPartnerIdentityStatus status) {
        return new BusinessPartnerIdentity(
            UUID.randomUUID(),
            BUSINESS_PARTNER_NAME,
            Instant.now(),
            "CHE-123-456-789",
            false,
            "de-CH",
            status,
            false,
            DateTimeHelper.today().plusYears(3).toInstant(), // 5 min
            null,
            Set.of("Some-did")
        );
    }

    public static TrustStatementPartnerLink partnerLinkIdentityV1(String subject) {
        var partnerLink = TrustStatementPartnerLink.createIdentityV1(
            UUID.randomUUID(),
            subject,
            Instant.now(),
            Instant.parse("2027-03-31T15:00:00Z"),
            BUSINESS_PARTNER_NAME,
            Collections.emptyList(),
            false
        );
        // set status to ACTIVE
        partnerLink.persistReferencesAfterPublicationSucceeded(
            UUID.randomUUID(),
            UUID.randomUUID(),
            TrustStatementPartnerLinkStatus.ACTIVE
        );
        return partnerLink;
    }

    public static TrustStatementPartnerLink partnerLinkIdentityV2(String subject) {
        var partnerLink = TrustStatementPartnerLink.createIdentityV2(
            UUID.randomUUID(),
            subject,
            Instant.now(),
            Instant.parse("2027-03-31T15:00:00Z"),
            BUSINESS_PARTNER_NAME,
            Collections.emptyList(),
            false,
            null
        );
        // set status to ACTIVE
        partnerLink.persistReferencesAfterPublicationSucceeded(
            UUID.randomUUID(),
            UUID.randomUUID(),
            TrustStatementPartnerLinkStatus.ACTIVE
        );
        return partnerLink;
    }
}
