package ch.admin.bj.swiyu.trust.management.modules.management.domain.task;

import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface DeleteBusinessPartnerTaskRepository extends JpaRepository<DeleteBusinessPartnerTask, UUID> {
    boolean existsByPartnerIdAndStatusIn(UUID partnerId, java.util.Collection<TaskStatus> statuses);
}
