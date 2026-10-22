package ch.admin.bj.swiyu.trust.management.modules.management.infrastructure.web.controller;

import static ch.admin.bj.swiyu.trust.management.modules.common.auth.UserRole.Expressions.HAS_ROLE_EDITOR_OR_READER;

import ch.admin.bj.swiyu.trust.management.modules.management.api.BusinessPartnerViewFilterDto;
import ch.admin.bj.swiyu.trust.management.modules.management.api.BusinessPartnerViewListItemDto;
import ch.admin.bj.swiyu.trust.management.modules.management.service.BusinessPartnerViewService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springdoc.core.annotations.ParameterObject;
import org.springdoc.core.converters.models.PageableAsQueryParam;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PagedModel;
import org.springframework.data.web.SortDefault;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Slf4j
@AllArgsConstructor
@Tag(name = "BusinessPartnerView")
@RestController
@RequestMapping("/ui-api/v1/business-partner-view")
public class BusinessPartnerViewController {

    private final BusinessPartnerViewService businessPartnerViewService;

    @GetMapping("/")
    @PreAuthorize(HAS_ROLE_EDITOR_OR_READER)
    @Operation(summary = "Get a paginated list of business partners.")
    @PageableAsQueryParam
    public PagedModel<BusinessPartnerViewListItemDto> getBusinessPartnerViews(
        @ParameterObject BusinessPartnerViewFilterDto filters,
        @SortDefault(sort = "partnerName", direction = Sort.Direction.ASC) @Parameter(
            hidden = true
        ) final Pageable pageable
    ) {
        return new PagedModel<>(this.businessPartnerViewService.getBusinessPartnerViews(filters, pageable));
    }
}
