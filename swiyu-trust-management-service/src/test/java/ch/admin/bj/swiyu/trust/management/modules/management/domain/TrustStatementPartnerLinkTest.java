package ch.admin.bj.swiyu.trust.management.modules.management.domain;

import static ch.admin.bj.swiyu.trust.management.modules.management.domain.TrustStatementPartnerLinkStatus.ACTIVE;
import static ch.admin.bj.swiyu.trust.management.modules.management.domain.TrustStatementPartnerLinkStatus.INACTIVE;
import static ch.admin.bj.swiyu.trust.management.modules.management.domain.details.TrustStatementPartnerLinkType.STATEMENT_TOMBSTONE;
import static org.assertj.core.api.Assertions.assertThat;

import ch.admin.bj.swiyu.trust.management.modules.management.domain.details.IdentityV2Details;
import ch.admin.bj.swiyu.trust.management.modules.management.domain.details.ProtectedIssuanceAuthorizationV2Details;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class TrustStatementPartnerLinkTest {

    private static final UUID PARTNER_ID = UUID.randomUUID();
    private static final UUID STATUS_LIST_METADATA_ID = UUID.randomUUID();
    private static final UUID TRUST_REGISTRY_ENTRY_ID = UUID.randomUUID();
    private static final UUID TRUST_ISSUER_CREDENTIAL_ID = UUID.randomUUID();
    private static final UUID PROTECTED_ISSUANCE_AUTHORIZATION_ID = UUID.randomUUID();
    private static final String SUBJECT = "did:example:123";
    private static final Instant VALID_FROM = Instant.parse("2026-01-01T00:00:00Z");
    private static final Instant VALID_UNTIL = Instant.parse("2027-01-01T00:00:00Z");

    @Test
    void convertToTombstone_removesPartnerContentAndKeepsTheStatusListReference() {
        var link = publishedIdentityV2();

        link.convertToTombstone();

        assertThat(link.getType()).isEqualTo(STATEMENT_TOMBSTONE);
        assertThat(link.getStatus()).isEqualTo(INACTIVE);
        assertThat(link.getSubject()).isNull();
        assertThat(link.getDetails()).isNull();
        assertThat(link.getTrustRegistryEntryId()).isNull();
        assertThat(link.getTrustIssuerCredentialId()).isNull();
        assertThat(link.getPartnerId()).isEqualTo(PARTNER_ID);
        assertThat(link.getStatusListMetadataId()).isEqualTo(STATUS_LIST_METADATA_ID);
        assertThat(link.getStatusListIndex()).isEqualTo(7);
        assertThat(link.getValidFrom()).isEqualTo(VALID_FROM);
        assertThat(link.getValidUntil()).isEqualTo(VALID_UNTIL);
    }

    @Test
    void convertToTombstone_clearsTheProtectedIssuanceAuthorizationReference() {
        var link = TrustStatementPartnerLink.createProtectedIssuanceAuthorizationV2(
            PARTNER_ID,
            SUBJECT,
            VALID_FROM,
            VALID_UNTIL,
            new ProtectedIssuanceAuthorizationV2Details.ProtectedIssuanceAuthorization(
                "https://example.com/vct",
                Map.of("default", "Example"),
                Map.of("default", "Reason")
            ),
            new StatusListEntry(STATUS_LIST_METADATA_ID, 3),
            PROTECTED_ISSUANCE_AUTHORIZATION_ID
        );
        link.persistReferencesAfterPublicationSucceeded(TRUST_ISSUER_CREDENTIAL_ID, TRUST_REGISTRY_ENTRY_ID, ACTIVE);

        link.convertToTombstone();

        assertThat(link.getType()).isEqualTo(STATEMENT_TOMBSTONE);
        assertThat(link.getProtectedIssuanceAuthorizationId()).isNull();
        assertThat(link.getStatusListMetadataId()).isEqualTo(STATUS_LIST_METADATA_ID);
        assertThat(link.getStatusListIndex()).isEqualTo(3);
    }

    @Test
    void convertToTombstone_isIdempotent() {
        var link = publishedIdentityV2();

        link.convertToTombstone();
        link.convertToTombstone();

        assertThat(link.getType()).isEqualTo(STATEMENT_TOMBSTONE);
        assertThat(link.getStatus()).isEqualTo(INACTIVE);
        assertThat(link.getSubject()).isNull();
        assertThat(link.getDetails()).isNull();
        assertThat(link.getStatusListIndex()).isEqualTo(7);
    }

    private static TrustStatementPartnerLink publishedIdentityV2() {
        var link = TrustStatementPartnerLink.createIdentityV2(
            PARTNER_ID,
            SUBJECT,
            VALID_FROM,
            VALID_UNTIL,
            Map.of("default", "Acme AG"),
            List.of(new IdentityV2Details.RegistryId("uid", "CHE-123.456.789")),
            true,
            new StatusListEntry(STATUS_LIST_METADATA_ID, 7)
        );
        link.persistReferencesAfterPublicationSucceeded(TRUST_ISSUER_CREDENTIAL_ID, TRUST_REGISTRY_ENTRY_ID, ACTIVE);
        return link;
    }
}
