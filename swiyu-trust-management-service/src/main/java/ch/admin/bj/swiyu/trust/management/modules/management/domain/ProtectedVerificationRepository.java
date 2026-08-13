package ch.admin.bj.swiyu.trust.management.modules.management.domain;

import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.querydsl.QuerydslPredicateExecutor;

public interface ProtectedVerificationRepository
    extends
        JpaRepository<ProtectedVerificationAuthorization, UUID>,
        QuerydslPredicateExecutor<ProtectedVerificationAuthorization>
{
    List<ProtectedVerificationAuthorization> findAllByBusinessPartnerIdentityId(UUID businessPartnerIdentityId);

    List<ProtectedVerificationAuthorization> findAllByBusinessPartnerIdentityIdAndProtectedVerificationField(
        UUID businessPartnerIdentityId,
        ProtectedVerificationField protectedVerificationField
    );

    void deleteByBusinessPartnerIdentityId(UUID businessPartnerIdentityId);
}
