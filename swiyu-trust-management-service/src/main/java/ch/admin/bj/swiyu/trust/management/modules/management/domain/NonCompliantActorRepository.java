package ch.admin.bj.swiyu.trust.management.modules.management.domain;

import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.querydsl.QuerydslPredicateExecutor;

public interface NonCompliantActorRepository
    extends JpaRepository<NonCompliantActor, UUID>, QuerydslPredicateExecutor<NonCompliantActor>
{
    boolean existsNonCompliantActorByDid(String did);
}
