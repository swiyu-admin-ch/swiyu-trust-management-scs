package ch.admin.bj.swiyu.trust.management.modules.management.domain;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.querydsl.QuerydslPredicateExecutor;

public interface NonCompliantActorRepository
    extends JpaRepository<NonCompliantActor, UUID>, QuerydslPredicateExecutor<NonCompliantActor>
{
    boolean existsNonCompliantActorByDid(String did);

    boolean existsNonCompliantActorByBusinessPartnerId(UUID businessPartnerId);

    Optional<NonCompliantActor> findNonCompliantActorByDid(String did);

    Optional<NonCompliantActor> findNonCompliantActorByBusinessPartnerId(UUID businessPartnerId);

    List<NonCompliantActor> findAllByBusinessPartnerId(UUID businessPartnerId);

    List<NonCompliantActor> findAllByDidIn(Collection<String> dids);
}
