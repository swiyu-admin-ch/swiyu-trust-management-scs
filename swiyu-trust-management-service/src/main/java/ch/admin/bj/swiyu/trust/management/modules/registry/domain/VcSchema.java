package ch.admin.bj.swiyu.trust.management.modules.registry.domain;

import ch.admin.bj.swiyu.trust.management.modules.common.audit.AuditMetadata;
import jakarta.persistence.*;
import jakarta.validation.Valid;
import java.util.UUID;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

@Entity
@NoArgsConstructor // JPA
@Getter
@Table(name = "vc_schema")
@EntityListeners(AuditingEntityListener.class)
public class VcSchema {

    @Embedded
    @Valid
    private final AuditMetadata audit = new AuditMetadata();

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "id")
    private UUID id;

    @Column(name = "path")
    private String path;

    @Column(name = "file")
    private String file;

    @Column(name = "status")
    @Enumerated(EnumType.STRING)
    private VcSchemaStatus status;

    public VcSchema(String path, String file) {
        this.path = path;
        this.file = file;
        this.status = VcSchemaStatus.ACTIVATED;
    }
}
