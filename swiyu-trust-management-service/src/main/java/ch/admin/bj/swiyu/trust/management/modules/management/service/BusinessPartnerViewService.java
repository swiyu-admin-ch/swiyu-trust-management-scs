package ch.admin.bj.swiyu.trust.management.modules.management.service;

import ch.admin.bj.swiyu.trust.management.modules.management.api.BusinessPartnerViewFilterDto;
import ch.admin.bj.swiyu.trust.management.modules.management.api.BusinessPartnerViewListItemDto;
import ch.admin.bj.swiyu.trust.management.modules.management.domain.BusinessPartnerViewRepository;
import ch.admin.bj.swiyu.trust.management.modules.management.domain.QBusinessPartnerView;
import com.querydsl.core.BooleanBuilder;
import com.querydsl.core.types.dsl.Expressions;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
public class BusinessPartnerViewService {

    private final BusinessPartnerViewRepository businessPartnerViewRepository;

    @Transactional(readOnly = true)
    public Page<BusinessPartnerViewListItemDto> getBusinessPartnerViews(
        BusinessPartnerViewFilterDto filter,
        Pageable pageable
    ) {
        QBusinessPartnerView q = QBusinessPartnerView.businessPartnerView;

        BooleanBuilder where = new BooleanBuilder();
        if (filter.searchText() != null && !filter.searchText().isBlank()) {
            var searchText = filter.searchText().trim();
            // Cast to text so that a fragment of a partner id matches as well as the whole one.
            var partnerIdAsText = Expressions.stringTemplate("cast({0} as string)", q.businessPartnerId);
            where.and(q.partnerName.containsIgnoreCase(searchText).or(partnerIdAsText.containsIgnoreCase(searchText)));
        }
        return businessPartnerViewRepository
            .findAll(where, pageable)
            .map(BusinessPartnerViewMapper::toBusinessPartnerViewListItemDto);
    }
}
