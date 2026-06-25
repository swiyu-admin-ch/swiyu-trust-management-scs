package ch.admin.bj.swiyu.trust.management.modules.management.domain;

import ch.admin.bj.swiyu.trust.management.modules.common.audit.AuditMetadata;
import jakarta.persistence.*;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.time.Instant;
import java.util.UUID;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

@Entity
@EntityListeners(AuditingEntityListener.class)
@Getter
@Table(name = "status_list_metadata")
@NoArgsConstructor // JPA
public class StatusListMetadata {

    @Embedded
    private final AuditMetadata audit = new AuditMetadata();

    @Id
    private UUID id;

    @Enumerated(EnumType.STRING)
    @Column(name = "status")
    private StatusListMetadataStatus status;

    @Column(name = "max_size")
    private int maxSize;

    @Column(name = "last_published_at")
    private Instant lastPublishedAt;

    @NotBlank
    @Column(name = "status_registry_url")
    private String statusRegistryUrl;

    /**
     * Column next_free_index MUST ONLY be retrieved/modified through domain service.
     * DO NOT add it as JPA column here as this messes with the atomic operation
     *
     * @see StatusListDomainService#getNewStatusListEntry
     */

    @Valid
    public StatusListMetadata(@NotNull UUID id, @NotBlank String statusRegistryUrl, int maxSize) {
        this.id = id;
        this.status = StatusListMetadataStatus.ACTIVE;
        this.statusRegistryUrl = statusRegistryUrl;
        this.maxSize = maxSize;
    }

    public void markAsFull() {
        this.status = StatusListMetadataStatus.FULL;
    }

    public void updateLastPublication() {
        this.lastPublishedAt = Instant.now();
    }

    public void markAsDeactivated() {
        this.status = StatusListMetadataStatus.DEACTIVATED;
    }
}
