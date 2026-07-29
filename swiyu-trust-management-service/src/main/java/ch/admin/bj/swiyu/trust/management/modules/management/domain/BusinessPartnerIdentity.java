package ch.admin.bj.swiyu.trust.management.modules.management.domain;

import ch.admin.bj.swiyu.messagetype.ti.BusinessPartnerIdentityStatus;
import ch.admin.bj.swiyu.trust.management.modules.common.audit.AuditMetadata;
import ch.admin.bj.swiyu.trust.management.modules.common.i18n.ValidLocalizedMap;
import jakarta.persistence.*;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.time.Instant;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

/**
 * Serves as an abstraction layer between the different input channels (TrustOnboardingSubmission, AddDidSubmission)
 * and the statements which express the current identity (IdentityTrustStatementV1,IdentityTrustStatementV2)
 */
@Entity
@EntityListeners(AuditingEntityListener.class)
@Valid
@NoArgsConstructor
@Getter
@Setter
@ToString
public class BusinessPartnerIdentity {

    @Id
    private UUID id;

    @NotNull
    @ValidLocalizedMap
    @Column(name = "partner_name", columnDefinition = "jsonb")
    @JdbcTypeCode(SqlTypes.JSON)
    private Map<String, @NotBlank String> entityName;

    private Instant lastActivated;

    private String uid;

    @NotNull
    private Boolean isRegisteredInCommercialRegister;

    private String correspondingLanguage;

    @Enumerated(EnumType.STRING)
    private BusinessPartnerIdentityStatus status;

    @NotNull
    private Boolean isStateActor;

    @NotNull
    @Column(name = "trusted_identifier", columnDefinition = "jsonb")
    @JdbcTypeCode(SqlTypes.JSON)
    private final Set<String> trustedIdentifier = new HashSet<>(); // DIDs to represent the BPI

    private Instant validUntil;

    private Instant lastIssuanceAt;

    @NotNull
    @Version
    private Long version;

    @Embedded
    private final AuditMetadata audit = new AuditMetadata();

    public BusinessPartnerIdentity(
        @NotNull UUID partnerId,
        Map<String, String> entityName,
        Instant lastActivated,
        String uid,
        boolean isRegisteredInCommercialRegister,
        String correspondingLanguage,
        BusinessPartnerIdentityStatus status,
        Boolean isStateActor,
        Instant validUntil,
        Instant lastIssuanceAt
    ) {
        this.id = partnerId;
        this.entityName = entityName;
        this.lastActivated = lastActivated;
        this.uid = uid;
        this.isRegisteredInCommercialRegister = isRegisteredInCommercialRegister;
        this.correspondingLanguage = correspondingLanguage;
        this.status = status;
        this.isStateActor = isStateActor;
        this.validUntil = validUntil;
        this.lastIssuanceAt = lastIssuanceAt;
    }
}
