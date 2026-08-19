package ch.admin.bj.swiyu.trust.management.modules.management.domain;

import jakarta.validation.constraints.NotNull;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.querydsl.QuerydslPredicateExecutor;

public interface ProtectedIssuanceAuthorizationRepository
    extends
        JpaRepository<ProtectedIssuanceAuthorization, UUID>,
        QuerydslPredicateExecutor<ProtectedIssuanceAuthorization>
{
    List<ProtectedIssuanceAuthorization> findAllByBusinessPartnerIdentityId(UUID businessPartnerIdentityId);

    List<ProtectedIssuanceAuthorization> findAllByProtectedIssuanceEntryId(UUID protectedIssuanceEntryId);

    void deleteByBusinessPartnerIdentityId(@NotNull UUID businessPartnerIdentityId);

    void deleteByProtectedIssuanceEntryId(UUID protectedIssuanceEntryId);
}
