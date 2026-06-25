package ch.admin.bj.swiyu.trust.management.test;

import ch.admin.bj.swiyu.trust.management.modules.management.api.IssuanceV1RequestDto;
import ch.admin.bj.swiyu.trust.management.modules.management.domain.TrustStatementPartnerLink;
import ch.admin.bj.swiyu.trust.management.modules.management.domain.details.IdentityV1Details;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public class TrustStatementTestData {

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

    public static IssuanceV1RequestDto issuanceV1Request(String subject) {
        return issuanceV1Request(
            subject,
            "https://trust-reg.trust-infra.swiyu.admin.ch/api/v1/vc-schema/test-vc-schema"
        );
    }

    public static IssuanceV1RequestDto issuanceV1Request(String subject, String canIssue) {
        return new IssuanceV1RequestDto(
            subject,
            Instant.now().minusSeconds(10),
            Instant.now().plusSeconds(10),
            canIssue
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
