/*
 * SPDX-FileCopyrightText: 2025 Swiss Confederation
 *
 * SPDX-License-Identifier: MIT
 */

package ch.admin.bj.swiyu.trust.management.modules.management.domain;

import ch.admin.bj.swiyu.trust.management.modules.management.domain.details.TrustStatementPartnerLinkType;
import java.time.Instant;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Stream;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.querydsl.QuerydslPredicateExecutor;

public interface TrustStatementPartnerLinkRepository
    extends JpaRepository<TrustStatementPartnerLink, UUID>, QuerydslPredicateExecutor<TrustStatementPartnerLink>
{
    Stream<TrustStatementPartnerLink> findAllBySubjectAndTypeAndStatusIsInAndValidFromBetweenAndValidUntilBetween(
        String subject,
        TrustStatementPartnerLinkType type,
        List<TrustStatementPartnerLinkStatus> trustStatementPartnerLinkStatuses,
        Instant validFromFrom,
        Instant validFromTo,
        Instant validUntilFrom,
        Instant validUntilTo
    );

    /**
     * Get all TrustStatementPartnerLinks where status and a search based
     * on the validFrom validUntil time frame (inclusive) matches.
     */
    Stream<TrustStatementPartnerLink> findAllByTypeAndStatusIsInAndValidFromLessThanEqualAndValidUntilGreaterThanEqual(
        TrustStatementPartnerLinkType type,
        List<TrustStatementPartnerLinkStatus> trustStatementPartnerLinkStatuses,
        Instant searchFrom,
        Instant searchUntil
    );

    Optional<TrustStatementPartnerLink> findByIdAndType(UUID id, TrustStatementPartnerLinkType type);

    List<TrustStatementPartnerLink> findAllBySubjectAndTypeInAndStatus(
        String subject,
        List<TrustStatementPartnerLinkType> type,
        TrustStatementPartnerLinkStatus status
    );

    /**
     * The status list index of every statement on the given status list having the given status. Only the index is
     * selected: the statement rows carry their details as jsonb, and a publication needs nothing but the index, so
     * loading the entities would read and deserialize every statement of the list for one integer.
     */
    @Query(
        """
        SELECT statement.statusListIndex
        FROM TrustStatementPartnerLink statement
        WHERE statement.statusListMetadataId = :statusListMetadataId
          AND statement.status = :status
          AND statement.statusListIndex IS NOT NULL
        """
    )
    List<Integer> findStatusListIndicesByStatusListMetadataIdAndStatus(
        UUID statusListMetadataId,
        TrustStatementPartnerLinkStatus status
    );

    long countByType(TrustStatementPartnerLinkType type);

    List<TrustStatementPartnerLink> findAllByTypeAndStatus(
        TrustStatementPartnerLinkType type,
        TrustStatementPartnerLinkStatus status
    );

    List<TrustStatementPartnerLink> findAllByTypeInAndStatus(
        List<TrustStatementPartnerLinkType> type,
        TrustStatementPartnerLinkStatus status
    );

    List<TrustStatementPartnerLink> findAllByPartnerIdAndStatus(
        UUID businessPartnerIdentityId,
        TrustStatementPartnerLinkStatus trustStatementPartnerLinkStatus
    );

    /**
     * All statements of one business partner, matched by the partner id and by the DIDs of its identity. The second
     * criterion exists because statements created before {@code partner_id} was introduced have no partner reference.
     */
    List<TrustStatementPartnerLink> findAllByPartnerIdOrSubjectIn(UUID partnerId, Collection<String> subjects);

    long countByTypeNot(TrustStatementPartnerLinkType type);

    List<TrustStatementPartnerLink> findAllByProtectedIssuanceAuthorizationIdAndStatus(
        UUID protectedIssuanceAuthorizationId,
        TrustStatementPartnerLinkStatus status
    );

    void deleteAllByPartnerId(UUID partnerId);

    boolean existsByIdOrTrustRegistryEntryId(UUID id, UUID trustRegistryEntryId);

    List<TrustStatementPartnerLink> findAllByPartnerId(UUID partnerId);
}
