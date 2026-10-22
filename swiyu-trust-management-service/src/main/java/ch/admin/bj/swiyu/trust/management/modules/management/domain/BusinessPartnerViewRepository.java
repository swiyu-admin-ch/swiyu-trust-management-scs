package ch.admin.bj.swiyu.trust.management.modules.management.domain;

import java.util.UUID;
import org.springframework.data.querydsl.QuerydslPredicateExecutor;
import org.springframework.data.repository.Repository;

/** Extends the bare {@link Repository} marker rather than {@code JpaRepository}: view rows must not be written. */
public interface BusinessPartnerViewRepository
    extends Repository<BusinessPartnerView, UUID>, QuerydslPredicateExecutor<BusinessPartnerView> {}
