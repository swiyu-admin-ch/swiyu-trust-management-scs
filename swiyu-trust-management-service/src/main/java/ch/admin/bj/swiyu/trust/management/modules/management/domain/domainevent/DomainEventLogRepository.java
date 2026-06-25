package ch.admin.bj.swiyu.trust.management.modules.management.domain.domainevent;

import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.querydsl.QuerydslPredicateExecutor;
import org.springframework.data.repository.query.Param;

public interface DomainEventLogRepository
    extends JpaRepository<DomainEventLog, UUID>, QuerydslPredicateExecutor<DomainEventLog>
{
    @Modifying
    @Query(
        "DELETE FROM DomainEventLog d WHERE d.trustTaskId IN (SELECT t.id FROM TrustTask t WHERE t.partnerId = :partnerId)"
    )
    void deleteAllByTrustTaskPartnerId(@Param("partnerId") UUID partnerId);
}
