package ch.admin.bj.swiyu.trust.management.modules.management.domain;

import ch.admin.bj.swiyu.trust.management.modules.common.audit.AuditMetadata;
import com.google.common.annotations.VisibleForTesting;
import jakarta.persistence.*;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import java.util.UUID;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.ToString;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

@Entity
@EntityListeners(AuditingEntityListener.class)
@Valid
@NoArgsConstructor
@Getter
@ToString
public class ProtectedVerificationAuthorization {

    @Id
    private UUID id;

    @Enumerated(EnumType.STRING)
    private ProtectedVerificationField protectedVerificationField;

    @NotNull
    private UUID businessPartnerIdentityId;

    @Embedded
    private final AuditMetadata audit = new AuditMetadata();

    public ProtectedVerificationAuthorization(
        UUID id,
        UUID businessPartnerIdentityId,
        ProtectedVerificationField protectedVerificationField
    ) {
        this.id = id;
        this.protectedVerificationField = protectedVerificationField;
        this.businessPartnerIdentityId = businessPartnerIdentityId;
    }

    @VisibleForTesting
    public void overrideFrom(ProtectedVerificationAuthorization source) {
        this.id = source.id;
        this.businessPartnerIdentityId = source.getBusinessPartnerIdentityId();
        this.protectedVerificationField = source.getProtectedVerificationField();
    }
}
