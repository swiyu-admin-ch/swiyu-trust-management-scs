package ch.admin.bj.swiyu.trust.management.modules.registry.domain;

import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface VcSchemaRepository extends JpaRepository<VcSchema, UUID> {
    Optional<VcSchema> findByPath(String path);
}
