/*
 * SPDX-FileCopyrightText: 2025 Swiss Confederation
 *
 * SPDX-License-Identifier: MIT
 */

package ch.admin.bj.swiyu.trust.management.modules.management.domain;

import ch.admin.bj.swiyu.trust.management.modules.management.domain.details.TrustStatementPartnerLinkType;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Stream;
import org.springframework.data.jpa.repository.JpaRepository;
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

    List<TrustStatementPartnerLink> findAllByStatusListMetadataIdAndStatus(
        UUID statuslistMetadataId,
        TrustStatementPartnerLinkStatus status
    );

    List<TrustStatementPartnerLink> findAllByTypeAndStatus(
        TrustStatementPartnerLinkType type,
        TrustStatementPartnerLinkStatus status
    );

    List<TrustStatementPartnerLink> findAllByTypeInAndStatus(
        List<TrustStatementPartnerLinkType> type,
        TrustStatementPartnerLinkStatus status
    );
}
