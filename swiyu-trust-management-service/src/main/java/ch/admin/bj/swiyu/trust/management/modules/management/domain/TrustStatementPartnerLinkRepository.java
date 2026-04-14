/*
 * SPDX-FileCopyrightText: 2025 Swiss Confederation
 *
 * SPDX-License-Identifier: MIT
 */

package ch.admin.bj.swiyu.trust.management.modules.management.domain;

import ch.admin.bj.swiyu.trust.management.modules.management.domain.details.TrustStatementType;
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
        TrustStatementType type,
        List<TrustStatementPartnerLinkStatus> trustStatementPartnerLinkStatuses,
        Instant validFromFrom,
        Instant validFromTo,
        Instant validUntilFrom,
        Instant validUntilTo
    );

    Optional<TrustStatementPartnerLink> findByIdAndType(UUID id, TrustStatementType type);

    Optional<TrustStatementPartnerLink> findBySubjectAndTypeAndStatus(
        String subject,
        TrustStatementType type,
        TrustStatementPartnerLinkStatus status
    );
}
