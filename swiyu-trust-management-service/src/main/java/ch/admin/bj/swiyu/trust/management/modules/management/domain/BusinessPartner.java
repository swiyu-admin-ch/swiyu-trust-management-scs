package ch.admin.bj.swiyu.trust.management.modules.management.domain;

import ch.admin.bj.swiyu.trust.management.modules.common.audit.AuditMetadata;
import jakarta.persistence.Column;
import jakarta.persistence.Embedded;
import jakarta.persistence.Entity;
import jakarta.persistence.EntityListeners;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import java.time.Instant;
import java.util.UUID;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.ToString;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

/**
 * Local projection of a business partner owned by the core business service, so the partner view can list partners
 * and target a deletion without asking for every row.
 *
 * <p>Refreshed from the authoritative {@code BusinessPartnerDto} on every sync and never edited by this service.
 */
@Entity
@EntityListeners(AuditingEntityListener.class)
@Valid
@NoArgsConstructor
@Getter
@ToString
public class BusinessPartner {

    @Embedded
    private final AuditMetadata audit = new AuditMetadata();

    /** The core business service partner id, not generated here. */
    @Id
    private UUID id;

    /** Flattened from the localized {@code entityName}; the language variants stay in the core business service. */
    @Column(name = "partner_name")
    private String name;

    private String uid;

    @Enumerated(EnumType.STRING)
    private BusinessPartnerType type;

    /** False until a sync says otherwise, so a projection that has never been refreshed offers no deletion. */
    @NotNull
    private boolean hardDeleteAllowed;

    /** When this projection was last refreshed; the hook for the nightly reconciliation. */
    @NotNull
    private Instant syncedAt;

    private BusinessPartner(UUID id) {
        this.id = id;
    }

    public static BusinessPartner createFor(UUID businessPartnerId) {
        return new BusinessPartner(businessPartnerId);
    }

    /** Overwrites every mirrored attribute, so the projection never mixes two points in time. */
    public void sync(String name, String uid, BusinessPartnerType type, boolean hardDeleteAllowed, Instant syncedAt) {
        this.name = name;
        this.uid = uid;
        this.type = type;
        this.hardDeleteAllowed = hardDeleteAllowed;
        this.syncedAt = syncedAt;
    }
}
