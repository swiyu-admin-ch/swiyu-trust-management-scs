package ch.admin.bj.swiyu.trust.management.modules.management.domain.task;

import java.util.Collection;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface DeleteBusinessPartnerTaskRepository extends JpaRepository<DeleteBusinessPartnerTask, UUID> {
    boolean existsByPartnerIdAndStatusIn(UUID partnerId, Collection<TaskStatus> statuses);

    /**
     * The latest deletion request of a partner, whatever its state. A pending request is always the latest one: the
     * partial unique index forbids creating another one while it is open, so a newer request can only exist once the
     * older one was approved or rejected.
     */
    Optional<DeleteBusinessPartnerTask> findFirstByPartnerIdOrderBySubmittedAtDesc(UUID partnerId);
}
