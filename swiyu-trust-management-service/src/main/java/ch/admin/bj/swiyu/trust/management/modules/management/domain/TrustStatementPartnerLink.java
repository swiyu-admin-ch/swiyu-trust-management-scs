package ch.admin.bj.swiyu.trust.management.modules.management.domain;

import ch.admin.bj.swiyu.trust.management.modules.common.audit.AuditMetadata;
import ch.admin.bj.swiyu.trust.management.modules.management.domain.details.*;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

@Entity
@EntityListeners(AuditingEntityListener.class)
@Getter
@Table(name = "trust_statement")
@NoArgsConstructor
@Inheritance(strategy = InheritanceType.JOINED)
public class TrustStatementPartnerLink {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    /**
     * The id of the business partner. Currently nullable only because:
     *
     * <ol>
     *      <li>TrustStatementMetadataV1 do not have a referencing partnerid</li>
     *      <li>for new TrustStatements (Identity, Issuance, Verification) it is not yet possible to resolve the partner id</li>
     * </ol>
     *
     * Once 1. is removed and 2. is solved by a better UI, we can make it nullable.
     */
    private UUID partnerId;

    @Enumerated(EnumType.STRING)
    private TrustStatementType type;

    private String subject;
    private Instant validFrom;
    private Instant validUntil;

    @Enumerated(EnumType.STRING)
    private TrustStatementPartnerLinkStatus status;

    private UUID trustRegistryEntryId = null;
    private UUID trustIssuerCredentialId = null;

    @Embedded
    private final AuditMetadata audit = new AuditMetadata();

    @Column(columnDefinition = "jsonb")
    @JdbcTypeCode(SqlTypes.JSON)
    @NotNull
    private TrustStatementDetails details;

    public static TrustStatementPartnerLink createMetadataV1(
        String subject,
        Instant validFrom,
        Instant validUntil,
        MetadataV1Details.Language preferredLanguage,
        Map<MetadataV1Details.Language, String> orgName,
        Map<MetadataV1Details.Language, String> logoUri
    ) {
        var type = TrustStatementType.TRUST_STATEMENT_METADATA_V1;
        return new TrustStatementPartnerLink(
            null, // for these legacy trust statement we don't start storing the partner id
            type,
            subject,
            validFrom,
            validUntil,
            new MetadataV1Details(preferredLanguage, orgName, logoUri)
        );
    }

    public static TrustStatementPartnerLink createIdentityV1(
        UUID partnerId,
        String subject,
        Instant validFrom,
        Instant validUntil,
        Map<IdentityV1Details.Language, String> entityName,
        List<IdentityV1Details.RegistryId> registryIds,
        Boolean isStateActor
    ) {
        var type = TrustStatementType.TRUST_STATEMENT_IDENTITY_V1;
        return new TrustStatementPartnerLink(
            partnerId,
            type,
            subject,
            validFrom,
            validUntil,
            new IdentityV1Details(entityName, isStateActor, registryIds)
        );
    }

    public static TrustStatementPartnerLink createIssuanceV1(
        UUID partnerId,
        String subject,
        Instant validFrom,
        Instant validUntil,
        String canIssue
    ) {
        var type = TrustStatementType.TRUST_STATEMENT_ISSUANCE_V1;
        return new TrustStatementPartnerLink(
            partnerId,
            type,
            subject,
            validFrom,
            validUntil,
            new IssuanceV1Details(canIssue)
        );
    }

    public static TrustStatementPartnerLink createVerificationV1(
        UUID partnerId,
        String subject,
        Instant validFrom,
        Instant validUntil,
        String canVerify
    ) {
        var type = TrustStatementType.TRUST_STATEMENT_VERIFICATION_V1;
        return new TrustStatementPartnerLink(
            partnerId,
            type,
            subject,
            validFrom,
            validUntil,
            new VerificationV1Details(canVerify)
        );
    }

    protected TrustStatementPartnerLink(
        UUID partnerId,
        TrustStatementType type,
        String subject,
        Instant validFrom,
        Instant validUntil,
        TrustStatementDetails details
    ) {
        this.status = TrustStatementPartnerLinkStatus.CONFIRMED;
        this.type = type;
        this.subject = subject;
        this.validUntil = validUntil;
        this.validFrom = validFrom;
        this.details = details;
    }

    public void persistReferencesAfterPublicationSucceeded(
        UUID trustIssuerCredentialId,
        UUID trustRegistryEntryId,
        TrustStatementPartnerLinkStatus newStatus
    ) {
        this.trustIssuerCredentialId = trustIssuerCredentialId;
        this.trustRegistryEntryId = trustRegistryEntryId;
        this.status = newStatus;
    }

    public void markAsInactive() {
        if (this.status != TrustStatementPartnerLinkStatus.ACTIVE) throw new IllegalStateException(
            "A trust statement can only move to inactive from an active state."
        );
        this.status = TrustStatementPartnerLinkStatus.INACTIVE;
    }

    public String getVct() {
        return getType().getVct();
    }

    public List<String> getMetadataCredentialSupportedIds() {
        return getType().getMetadataCredentialSupportedIds();
    }

    public boolean isNew() {
        return id == null;
    }
}
