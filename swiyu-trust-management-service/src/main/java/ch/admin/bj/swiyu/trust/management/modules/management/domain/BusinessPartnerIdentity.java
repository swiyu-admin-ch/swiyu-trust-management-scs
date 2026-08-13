package ch.admin.bj.swiyu.trust.management.modules.management.domain;

import static ch.admin.bj.swiyu.trust.management.modules.common.date.DateTimeHelper.today;

import ch.admin.bj.swiyu.messagetype.ti.BusinessPartnerIdentityStatus;
import ch.admin.bj.swiyu.trust.management.modules.common.audit.AuditMetadata;
import ch.admin.bj.swiyu.trust.management.modules.common.i18n.ValidLocalizedMap;
import com.google.common.annotations.VisibleForTesting;
import jakarta.persistence.*;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.time.Instant;
import java.time.Period;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import lombok.Getter;
import lombok.NoArgsConstructor;
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

    public void activate(Period statementValidity) {
        status = BusinessPartnerIdentityStatus.ACTIVE;
        validUntil = calculateValidUntilFromNow(statementValidity);
        lastActivated = Instant.now();
    }

    public void deactivate() {
        status = BusinessPartnerIdentityStatus.DEACTIVATED;
    }

    public void updateLastIssuance() {
        lastIssuanceAt = Instant.now();
    }

    private Instant calculateValidUntilFromNow(Period statementValidity) {
        return today().plus(statementValidity).toInstant();
    }

    @VisibleForTesting // only used for unit test
    public void setVersion(long version) {
        this.version = version;
    }

    @VisibleForTesting
    public void overrideFrom(BusinessPartnerIdentity source) {
        this.id = source.id;
        this.entityName = source.entityName;
        this.lastActivated = source.lastActivated;
        this.uid = source.uid;
        this.isRegisteredInCommercialRegister = source.isRegisteredInCommercialRegister;
        this.correspondingLanguage = source.correspondingLanguage;
        this.status = source.status;
        this.isStateActor = source.isStateActor;
        this.validUntil = source.validUntil;
        this.lastIssuanceAt = source.lastIssuanceAt;
    }
}
