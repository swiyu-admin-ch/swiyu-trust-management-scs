package ch.admin.bj.swiyu.trust.management.modules.management.domain;

import ch.admin.bj.swiyu.trust.management.modules.common.audit.AuditMetadata;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
import java.util.UUID;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

@Entity
@EntityListeners(AuditingEntityListener.class)
@Getter
@Table(name = "vc_schema_partner_link")
@NoArgsConstructor
public class VcSchemaPartnerLink {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Embedded
    @NotNull
    private final AuditMetadata auditMetadata = new AuditMetadata();

    @NotNull
    @Column(name = "vc_schema_id")
    private UUID vcSchemaId;

    @NotNull
    @Column(name = "vc_schema_submission_id")
    private UUID vcSchemaSubmissionId;

    @NotNull
    @Column(name = "partner_id")
    private UUID partnerId;

    public VcSchemaPartnerLink(UUID vcSchemaId, UUID vcSchemaSubmissionId, UUID partnerId) {
        this.vcSchemaId = vcSchemaId;
        this.vcSchemaSubmissionId = vcSchemaSubmissionId;
        this.partnerId = partnerId;
    }
}
