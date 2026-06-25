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

    @Embedded
    private final AuditMetadata audit = new AuditMetadata();

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    /**
     * The id of the business partner. Currently nullable only because:
     * <ol>
     *      <li>TrustStatementMetadataV1 do not have a referencing partnerid</li>*
     * </ol>
     * <p>
     * Once this limitation is removed we can make it NOT NULL.
     */
    private UUID partnerId;

    @Enumerated(EnumType.STRING)
    private TrustStatementPartnerLinkType type;

    private String subject;
    private Instant validFrom;
    private Instant validUntil;

    @Enumerated(EnumType.STRING)
    private TrustStatementPartnerLinkStatus status;

    private UUID trustRegistryEntryId = null;
    private UUID trustIssuerCredentialId = null;

    @Column(columnDefinition = "jsonb")
    @JdbcTypeCode(SqlTypes.JSON)
    @NotNull
    private TrustStatementDetails details;

    @Column(name = "status_list_metadata_id")
    private UUID statusListMetadataId;

    @Column(name = "status_list_index")
    private Integer statusListIndex;

    /**
     *
     * @param partnerId       BusinessPartnerId to whom the statement belongs to
     * @param type            Type of the TrustStatementPartnerLink
     * @param subject         Subject of the statement
     * @param validFrom       Not-valid-before time frame of statement
     * @param validUntil      Expiry time frame of statement
     * @param details         Statement details, based on the type
     * @param statusListEntry Statements statuslist position
     */
    protected TrustStatementPartnerLink(
        UUID partnerId,
        TrustStatementPartnerLinkType type,
        String subject,
        Instant validFrom,
        Instant validUntil,
        TrustStatementDetails details,
        StatusListEntry statusListEntry
    ) {
        this.status = TrustStatementPartnerLinkStatus.CONFIRMED;
        this.partnerId = partnerId;
        this.type = type;
        this.subject = subject;
        this.validUntil = validUntil;
        this.validFrom = validFrom;
        this.details = details;
        this.statusListIndex = null;
        this.statusListMetadataId = null;
        if (statusListEntry != null) {
            this.statusListIndex = statusListEntry.allocatedIndex();
            this.statusListMetadataId = statusListEntry.statusListMetadataId();
        }
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
        var type = TrustStatementPartnerLinkType.TRUST_STATEMENT_IDENTITY_V1;
        return new TrustStatementPartnerLink(
            partnerId,
            type,
            subject,
            validFrom,
            validUntil,
            new IdentityV1Details(entityName, isStateActor, registryIds),
            null
        );
    }

    public static TrustStatementPartnerLink createIdentityV2(
        UUID partnerId,
        String subject,
        Instant validFrom,
        Instant validUntil,
        Map<IdentityV2Details.Language, String> entityName,
        List<IdentityV2Details.RegistryId> registryIds,
        Boolean isStateActor,
        StatusListEntry newStatusListEntry
    ) {
        var type = TrustStatementPartnerLinkType.TRUST_STATEMENT_IDENTITY_V2;
        return new TrustStatementPartnerLink(
            partnerId,
            type,
            subject,
            validFrom,
            validUntil,
            new IdentityV2Details(entityName, isStateActor, registryIds),
            newStatusListEntry
        );
    }

    public static TrustStatementPartnerLink createVerificationQueryV2(
        UUID partnerId,
        String subject,
        Instant validFrom,
        Instant validUntil,
        Map<VerificationQueryV2Details.Language, String> purposeName,
        Map<VerificationQueryV2Details.Language, String> purposeDescription,
        VerificationQueryV2Details.VerificationRequestObject request
    ) {
        var type = TrustStatementPartnerLinkType.PUBLIC_STATEMENT_VERIFICATION_QUERY_V2;
        return new TrustStatementPartnerLink(
            partnerId,
            type,
            subject,
            validFrom,
            validUntil,
            new VerificationQueryV2Details(purposeName, purposeDescription, request),
            null
        );
    }

    public static TrustStatementPartnerLink createNonComplianceV2(
        UUID partnerId,
        Instant validFrom,
        Instant validUntil,
        List<NonCompliantActor> nonCompliantActors,
        StatusListEntry newStatusListEntry
    ) {
        var type = TrustStatementPartnerLinkType.TRUST_LIST_STATEMENT_NON_COMPLIANCE_V2;
        return new TrustStatementPartnerLink(
            partnerId,
            type,
            null,
            validFrom,
            validUntil,
            new NonComplianceV2Details(NonCompliantActorMapper.toNonComplianceV2Details(nonCompliantActors)),
            newStatusListEntry
        );
    }

    public static TrustStatementPartnerLink createProtectedVerificationAuthorizationV2(
        UUID partnerId,
        String subject,
        Instant validFrom,
        Instant validUntil,
        List<ProtectedVerificationAuthorizationV2Details.AuthorizableField> authorizedFields,
        StatusListEntry newStatusListEntry
    ) {
        var type = TrustStatementPartnerLinkType.TRUST_STATEMENT_PROTECTED_VERIFICATION_AUTHORIZATION_V2;
        return new TrustStatementPartnerLink(
            partnerId,
            type,
            subject,
            validFrom,
            validUntil,
            new ProtectedVerificationAuthorizationV2Details(authorizedFields),
            newStatusListEntry
        );
    }

    public static TrustStatementPartnerLink createProtectedIssuanceAuthorizationV2(
        UUID partnerId,
        String subject,
        Instant validFrom,
        Instant validUntil,
        ProtectedIssuanceAuthorizationV2Details.ProtectedIssuanceAuthorization canIssue,
        StatusListEntry newStatusListEntry
    ) {
        var type = TrustStatementPartnerLinkType.TRUST_STATEMENT_PROTECTED_ISSUANCE_AUTHORIZATION_V2;
        return new TrustStatementPartnerLink(
            partnerId,
            type,
            subject,
            validFrom,
            validUntil,
            new ProtectedIssuanceAuthorizationV2Details(canIssue),
            newStatusListEntry
        );
    }

    public static TrustStatementPartnerLink createIssuanceV1(
        UUID partnerId,
        String subject,
        Instant validFrom,
        Instant validUntil,
        String canIssue
    ) {
        var type = TrustStatementPartnerLinkType.TRUST_STATEMENT_ISSUANCE_V1;
        return new TrustStatementPartnerLink(
            partnerId,
            type,
            subject,
            validFrom,
            validUntil,
            new IssuanceV1Details(canIssue),
            null
        );
    }

    public static TrustStatementPartnerLink createVerificationV1(
        UUID partnerId,
        String subject,
        Instant validFrom,
        Instant validUntil,
        String canVerify
    ) {
        var type = TrustStatementPartnerLinkType.TRUST_STATEMENT_VERIFICATION_V1;
        return new TrustStatementPartnerLink(
            partnerId,
            type,
            subject,
            validFrom,
            validUntil,
            new VerificationV1Details(canVerify),
            null
        );
    }

    public static TrustStatementPartnerLink createProtectedIssuanceV2(
        UUID partnerId,
        Instant validFrom,
        Instant validUntil,
        List<String> protectedElements,
        StatusListEntry newStatusListEntry
    ) {
        var type = TrustStatementPartnerLinkType.TRUST_LIST_STATEMENT_PROTECTED_ISSUANCE_V2;
        return new TrustStatementPartnerLink(
            partnerId,
            type,
            null,
            validFrom,
            validUntil,
            new ProtectedIssuanceV2Details(protectedElements),
            newStatusListEntry
        );
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
        if (this.status != TrustStatementPartnerLinkStatus.ACTIVE) {
            throw new IllegalStateException("A trust statement can only move to inactive from an active state.");
        }
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

    public boolean isV2PartnerLink() {
        return switch (this.type) {
            case
                TRUST_STATEMENT_IDENTITY_V2,
                TRUST_LIST_STATEMENT_NON_COMPLIANCE_V2,
                TRUST_LIST_STATEMENT_PROTECTED_ISSUANCE_V2,
                TRUST_STATEMENT_PROTECTED_VERIFICATION_AUTHORIZATION_V2,
                PUBLIC_STATEMENT_VERIFICATION_QUERY_V2,
                TRUST_STATEMENT_PROTECTED_ISSUANCE_AUTHORIZATION_V2 -> true;
            default -> false;
        };
    }
}
