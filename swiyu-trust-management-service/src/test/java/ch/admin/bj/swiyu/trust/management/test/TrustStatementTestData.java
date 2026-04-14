package ch.admin.bj.swiyu.trust.management.test;

import static ch.admin.bj.swiyu.trust.management.modules.management.domain.details.MetadataV1Details.Language.*;
import static ch.admin.bj.swiyu.trust.management.test.RequestTestData.logoUri;

import ch.admin.bj.swiyu.trust.management.modules.management.domain.TrustStatementPartnerLink;
import ch.admin.bj.swiyu.trust.management.modules.management.domain.details.IdentityV1Details;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public class TrustStatementTestData {

    public static TrustStatementPartnerLink metadataV1(String subject) {
        return TrustStatementPartnerLink.createMetadataV1(
            subject,
            Instant.now(),
            Instant.now().plusSeconds(3600),
            DE_CH,
            Map.of(
                DE_CH,
                "Org Name DE",
                EN,
                "Org Name EN",
                FR_CH,
                "Nom de l'org FR",
                IT_CH,
                "Nome dell'org IT",
                RM_CH,
                "Numn da l'org RM"
            ),
            Map.of(DE_CH, logoUri(), EN, logoUri(), FR_CH, logoUri(), IT_CH, logoUri(), RM_CH, logoUri())
        );
    }

    public static TrustStatementPartnerLink identityV1(String subject) {
        return TrustStatementPartnerLink.createIdentityV1(
            UUID.randomUUID(),
            subject,
            Instant.parse("2025-01-01T00:00:00Z"),
            Instant.parse("2026-01-01T00:00:00Z"),
            Map.of(
                IdentityV1Details.Language.DE_CH,
                "Entity Name DE",
                IdentityV1Details.Language.EN,
                "Entity Name EN",
                IdentityV1Details.Language.FR_CH,
                "Nom de l'entité FR",
                IdentityV1Details.Language.IT_CH,
                "Nome dell'entità IT",
                IdentityV1Details.Language.RM_CH,
                "Numn da l'entità RM"
            ),
            List.of(
                new IdentityV1Details.RegistryId("registryType1", "registryValue1"),
                new IdentityV1Details.RegistryId("registryType2", "registryValue2")
            ),
            true
        );
    }

    public static TrustStatementPartnerLink issuanceV1(String subject) {
        return issuanceV1(subject, "https://trust-reg.trust-infra.swiyu.admin.ch/api/v1/vc-schema/test-vc-schema");
    }

    public static TrustStatementPartnerLink issuanceV1(String subject, String canIssue) {
        return TrustStatementPartnerLink.createIssuanceV1(
            UUID.randomUUID(),
            subject,
            Instant.parse("2025-01-01T00:00:00Z"),
            Instant.parse("2026-01-01T00:00:00Z"),
            canIssue
        );
    }

    public static TrustStatementPartnerLink verificationV1(String subject) {
        return verificationV1(subject, "https://trust-reg.trust-infra.swiyu.admin.ch/api/v1/vc-schema/test-vc-schema");
    }

    public static TrustStatementPartnerLink verificationV1(String subject, String canVerify) {
        return TrustStatementPartnerLink.createVerificationV1(
            UUID.randomUUID(),
            subject,
            Instant.parse("2025-01-01T00:00:00Z"),
            Instant.parse("2026-01-01T00:00:00Z"),
            canVerify
        );
    }
}
