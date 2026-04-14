package ch.admin.bj.swiyu.trust.management.modules.management.domain;

import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface VcSchemaPartnerLinkRepository extends JpaRepository<VcSchemaPartnerLink, UUID> {
    boolean existsByVcSchemaId(UUID vcSchemaId);
}
