package ch.admin.bj.swiyu.trust.management.modules.management.domain;

import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.querydsl.QuerydslPredicateExecutor;

public interface ProtectedIssuanceEntryRepository
    extends JpaRepository<ProtectedIssuanceEntry, UUID>, QuerydslPredicateExecutor<ProtectedIssuanceEntry>
{
    Optional<ProtectedIssuanceEntry> findByVct(String vct);

    ProtectedIssuanceEntry getByVct(String vct);
}
