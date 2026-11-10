package ch.admin.bj.swiyu.trust.management.modules.management.domain;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.querydsl.QuerydslPredicateExecutor;

public interface BusinessPartnerIdentityRepository
    extends JpaRepository<BusinessPartnerIdentity, UUID>, QuerydslPredicateExecutor<BusinessPartnerIdentity>
{
    @Query(
        value = """
        SELECT *
        FROM business_partner_identity bpi
        WHERE jsonb_exists(bpi.trusted_identifier, :identifier)
        """,
        nativeQuery = true
    )
    Optional<BusinessPartnerIdentity> findByTrustedIdentifier(String identifier);

    long countByStatus(BusinessPartnerIdentityStatus status);

    List<BusinessPartnerIdentity> findAllByStatusAndValidUntilLessThanEqual(
        BusinessPartnerIdentityStatus status,
        Instant validUntilLimit
    );

    List<BusinessPartnerIdentity> findAllByStatusAndLastIssuanceAtLessThanEqual(
        BusinessPartnerIdentityStatus status,
        Instant lastIssuanceLimit
    );
}
