package ch.admin.bj.swiyu.trust.management.modules.management.domain;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.querydsl.QuerydslPredicateExecutor;

public interface StatusListMetadataRepository
    extends JpaRepository<StatusListMetadata, UUID>, QuerydslPredicateExecutor<StatusListMetadata>
{
    /**
     * Returns a StatusListEntry if a statuslistMetadata was found which is ACTIVE and has a free index
     */
    @Query(
        value = """
         WITH one_row AS (
            SELECT id
            FROM status_list_metadata
            WHERE next_free_index < max_size
              AND status = 'ACTIVE'
            ORDER BY id
            LIMIT 1
            FOR UPDATE SKIP LOCKED
        )
        UPDATE status_list_metadata slm
        SET next_free_index = next_free_index + 1
        FROM one_row
        WHERE slm.id = one_row.id
        RETURNING
            slm.id AS statusListMetadataId,
        slm.next_free_index - 1 AS allocatedIndex;
        """,
        nativeQuery = true
    )
    Optional<StatusListEntry> incrementAndGetIndex(); // NOSONAR false positive @Modifying cannot be used in conjunction with RETURNING

    List<StatusListMetadata> findAllByStatusIn(List<StatusListMetadataStatus> status);
}
