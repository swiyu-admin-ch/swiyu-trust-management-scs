package ch.admin.bj.swiyu.trust.management.modules.management.domain;

import ch.admin.bj.swiyu.trust.management.modules.common.audit.AuditMetadata;
import ch.admin.bj.swiyu.trust.management.modules.common.i18n.ValidLocalizedMap;
import jakarta.persistence.*;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.util.Map;
import java.util.UUID;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

@Entity
@EntityListeners(AuditingEntityListener.class)
@Valid
@NoArgsConstructor
@Getter
@Table(name = "protected_issuance_authorization")
public class ProtectedIssuanceAuthorization {

    @Embedded
    private final AuditMetadata audit = new AuditMetadata();

    @Id
    @Column(name = "id")
    private UUID id;

    @NotNull
    @Column(name = "business_partner_identity_id")
    private UUID businessPartnerIdentityId;

    @NotNull
    @Column(name = "protected_issuance_entry_id")
    private UUID protectedIssuanceEntryId;

    @ValidLocalizedMap
    @Column(name = "reason", columnDefinition = "jsonb")
    @JdbcTypeCode(SqlTypes.JSON)
    private Map<@NotBlank String, @NotBlank String> reason;

    public ProtectedIssuanceAuthorization(
        @NotNull UUID id,
        @NotNull UUID businessPartnerIdentityId,
        @NotNull UUID protectedIssuanceEntryId,
        @ValidLocalizedMap @NotNull Map<@NotBlank String, @NotBlank String> reason
    ) {
        this.id = id;
        this.businessPartnerIdentityId = businessPartnerIdentityId;
        this.protectedIssuanceEntryId = protectedIssuanceEntryId;
        this.reason = reason;
    }
}
