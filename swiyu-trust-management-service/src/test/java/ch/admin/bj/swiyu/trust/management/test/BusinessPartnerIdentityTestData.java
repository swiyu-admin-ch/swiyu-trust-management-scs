package ch.admin.bj.swiyu.trust.management.test;

import static ch.admin.bj.swiyu.trust.management.modules.common.date.DateTimeHelper.today;

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
            today().plusYears(3).toInstant(),
            null,
            Set.of("Some-did")
        );
    }

    /**
     * An active identity that expires in 30 days - the state in which the core-business-service offers a
     * mandatory profile change or a renewal, mirroring the demo partners on DEV.
     */
    public static BusinessPartnerIdentity businessPartnerIdentityExpiringIn30Days() {
        return new BusinessPartnerIdentity(
            UUID.randomUUID(),
            BUSINESS_PARTNER_NAME,
            today().minusYears(3).toInstant(),
            "CHE-123-456-789",
            true,
            "de-CH",
            BusinessPartnerIdentityStatus.ACTIVE,
            false,
            today().plusDays(30).toInstant(),
            null,
            Set.of("did1", "did2")
        );
    }

    public static BusinessPartnerIdentity businessPartnerIdentityLastIssuance7MonthsAgo() {
        return businessPartnerIdentityLastIssuance7MonthsAgo(BusinessPartnerIdentityStatus.ACTIVE);
    }

    public static BusinessPartnerIdentity businessPartnerIdentityLastIssuance7MonthsAgo(
        BusinessPartnerIdentityStatus status
    ) {
        return new BusinessPartnerIdentity(
            UUID.randomUUID(),
            BUSINESS_PARTNER_NAME,
            today().minusYears(3).toInstant(),
            "CHE-123-456-789",
            false,
            "de-CH",
            status,
            false,
            today().plusYears(3).toInstant(),
            today().minusMonths(7).toInstant(),
            Set.of("Some-did")
        );
    }

    /**
     * An active identity whose validity runs out <b>today</b> - the boundary case, because an identity is activated
     * with a midnight-aligned {@code validUntil} and therefore lands exactly on it.
     */
    public static BusinessPartnerIdentity businessPartnerIdentityValidUntilToday() {
        return new BusinessPartnerIdentity(
            UUID.randomUUID(),
            BUSINESS_PARTNER_NAME,
            today().minusYears(3).toInstant(),
            "CHE-123-456-789",
            false,
            "de-CH",
            BusinessPartnerIdentityStatus.ACTIVE,
            false,
            today().toInstant(),
            null,
            Set.of("Some-did")
        );
    }

    public static BusinessPartnerIdentity businessPartnerIdentityValidUntilYesterday() {
        return businessPartnerIdentityValidUntilYesterday(BusinessPartnerIdentityStatus.ACTIVE);
    }

    public static BusinessPartnerIdentity businessPartnerIdentityValidUntilYesterday(
        BusinessPartnerIdentityStatus status
    ) {
        return new BusinessPartnerIdentity(
            UUID.randomUUID(),
            BUSINESS_PARTNER_NAME,
            today().minusYears(3).minusDays(1).toInstant(),
            "CHE-123-456-789",
            false,
            "de-CH",
            status,
            false,
            today().minusDays(1).toInstant(),
            null,
            Set.of("Some-did")
        );
    }

    /**
     * An ACTIVE identity statement belonging to the given partner - needed wherever a test has to observe whether the
     * statements of an identity survive something or get revoked.
     */
    public static TrustStatementPartnerLink activePartnerLinkIdentityV1(UUID partnerId, String subject) {
        var partnerLink = TrustStatementPartnerLink.createIdentityV1(
            partnerId,
            subject,
            Instant.now(),
            today().plusYears(3).toInstant(),
            BUSINESS_PARTNER_NAME,
            Collections.emptyList(),
            false
        );
        partnerLink.persistReferencesAfterPublicationSucceeded(
            UUID.randomUUID(),
            UUID.randomUUID(),
            TrustStatementPartnerLinkStatus.ACTIVE
        );
        return partnerLink;
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
