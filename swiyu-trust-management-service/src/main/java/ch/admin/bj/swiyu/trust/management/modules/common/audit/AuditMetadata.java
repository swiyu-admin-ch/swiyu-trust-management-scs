package ch.admin.bj.swiyu.trust.management.modules.common.audit;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import java.time.Instant;
import lombok.*;
import org.springframework.data.annotation.CreatedBy;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedBy;
import org.springframework.data.annotation.LastModifiedDate;

/**
 * Embeddable to provide auto tracking of audit data for an entity in the DB.
 */
@Embeddable
@Getter
@ToString
@AllArgsConstructor
@NoArgsConstructor(access = AccessLevel.PUBLIC) // JPA
public class AuditMetadata {

    @Column(nullable = false)
    @LastModifiedDate
    private Instant lastModifiedAt;

    @Column(nullable = false)
    @LastModifiedBy
    private String lastModifiedBy;

    @Column(nullable = false, updatable = false)
    @CreatedDate
    private Instant createdAt;

    @Column(nullable = false, updatable = false)
    @CreatedBy
    private String createdBy;
}
