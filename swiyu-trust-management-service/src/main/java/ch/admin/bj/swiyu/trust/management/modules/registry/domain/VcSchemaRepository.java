package ch.admin.bj.swiyu.trust.management.modules.registry.domain;

import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface VcSchemaRepository extends JpaRepository<VcSchema, Long> {
    Optional<VcSchema> findByPath(String path);
}
