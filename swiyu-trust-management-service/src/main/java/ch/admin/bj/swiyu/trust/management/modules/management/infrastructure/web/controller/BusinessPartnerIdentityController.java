package ch.admin.bj.swiyu.trust.management.modules.management.infrastructure.web.controller;

import static ch.admin.bj.swiyu.trust.management.modules.common.auth.UserRole.Expressions.*;

import ch.admin.bj.swiyu.trust.management.modules.management.api.BusinessPartnerIdentityDto;
import ch.admin.bj.swiyu.trust.management.modules.management.api.BusinessPartnerIdentityFilterDto;
import ch.admin.bj.swiyu.trust.management.modules.management.service.BusinessPartnerIdentityService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import java.util.UUID;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springdoc.core.annotations.ParameterObject;
import org.springdoc.core.converters.models.PageableAsQueryParam;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PagedModel;
import org.springframework.data.web.SortDefault;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@Slf4j
@AllArgsConstructor
@Tag(name = "BusinessPartnerIdentity")
@RestController
@RequestMapping("/ui-api/v1/business-partner-identity")
public class BusinessPartnerIdentityController {

    private final BusinessPartnerIdentityService businessPartnerIdentityService;

    @GetMapping("/")
    @PreAuthorize(HAS_ROLE_EDITOR_OR_READER)
    @Operation(summary = "Get a paginated list of business partner identity")
    @PageableAsQueryParam
    public PagedModel<BusinessPartnerIdentityDto> getBusinessPartnerIdentities(
        @ParameterObject BusinessPartnerIdentityFilterDto filters,
        @SortDefault(sort = "updatedAt", direction = Sort.Direction.DESC) @Parameter(
            hidden = true
        ) final Pageable pageable
    ) {
        return new PagedModel<>(this.businessPartnerIdentityService.getBusinessPartnerIdentities(filters, pageable));
    }

    @GetMapping("/{businessPartnerIdentityId}")
    @PreAuthorize(HAS_ROLE_EDITOR_OR_READER)
    @Operation(summary = "Get a business partner identity.")
    public BusinessPartnerIdentityDto getBusinessPartnerIdentity(
        @PathVariable @Valid @NotNull UUID businessPartnerIdentityId
    ) {
        return this.businessPartnerIdentityService.getBusinessPartnerIdentityDto(businessPartnerIdentityId);
    }

    @PostMapping("/{businessPartnerIdentityId}/activate")
    @PreAuthorize(HAS_ROLE_EDITOR)
    @Operation(summary = "Activate a business partner identity.")
    public void activateBusinessPartnerIdentity(@PathVariable @Valid @NotNull UUID businessPartnerIdentityId) {
        this.businessPartnerIdentityService.activate(businessPartnerIdentityId);
    }

    @PostMapping("/{businessPartnerIdentityId}/deactivate")
    @PreAuthorize(HAS_ROLE_EDITOR)
    @Operation(summary = "Deactivate a business partner identity.")
    public void deactivateBusinessPartnerIdentity(@PathVariable @Valid @NotNull UUID businessPartnerIdentityId) {
        this.businessPartnerIdentityService.deactivate(businessPartnerIdentityId);
    }
}
