package ch.admin.bj.swiyu.trust.management.modules.management.domain;

import ch.admin.bj.swiyu.trust.management.modules.common.audit.AuditMetadata;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
import java.time.Instant;
import java.util.UUID;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

@Entity
@EntityListeners(AuditingEntityListener.class)
@Getter
@Table(name = "non_compliant_actor")
@NoArgsConstructor
public class NonCompliantActor {

    @Embedded
    private final AuditMetadata audit = new AuditMetadata();

    @Id
    private UUID id;

    @NotNull
    @Version
    private Long version;

    @NotNull
    private String did;

    private Instant flaggedAsNonCompliantAt;

    @Embedded
    private NonCompliantReasonText reason;

    public NonCompliantActor(UUID id, String did, NonCompliantReasonText reason) {
        this.id = id;
        this.did = did;
        this.reason = reason;
        this.flaggedAsNonCompliantAt = Instant.now();
    }
}
