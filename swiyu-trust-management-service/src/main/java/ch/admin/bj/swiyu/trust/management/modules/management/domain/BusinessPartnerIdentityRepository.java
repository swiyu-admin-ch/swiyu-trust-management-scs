package ch.admin.bj.swiyu.trust.management.modules.management.domain;

import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.querydsl.QuerydslPredicateExecutor;

public interface BusinessPartnerIdentityRepository
    extends JpaRepository<BusinessPartnerIdentity, UUID>, QuerydslPredicateExecutor<BusinessPartnerIdentity> {}
