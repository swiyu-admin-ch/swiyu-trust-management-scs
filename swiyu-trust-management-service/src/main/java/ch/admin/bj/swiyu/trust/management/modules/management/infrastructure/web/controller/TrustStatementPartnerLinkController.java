package ch.admin.bj.swiyu.trust.management.modules.management.infrastructure.web.controller;

import ch.admin.bj.swiyu.trust.management.modules.common.auth.UserRoles;
import ch.admin.bj.swiyu.trust.management.modules.management.api.TrustStatementPartnerLinkDeactivationRequestDto;
import ch.admin.bj.swiyu.trust.management.modules.management.api.TrustStatementPartnerLinkDto;
import ch.admin.bj.swiyu.trust.management.modules.management.api.TrustStatementPartnerLinkFilterDto;
import ch.admin.bj.swiyu.trust.management.modules.management.api.TrustStatementPartnerLinkListItemDto;
import ch.admin.bj.swiyu.trust.management.modules.management.service.TrustStatementService;
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
@Tag(name = "TrustStatement")
@RestController
@RequestMapping("/api/v1/trust-statement-partner-links")
public class TrustStatementPartnerLinkController {

    private final TrustStatementService trustStatementService;

    @GetMapping("/")
    @PreAuthorize("hasAnyRole('" + UserRoles.READER + "', '" + UserRoles.EDITOR + "')")
    @Operation(summary = "IF-012.001 - Get a paginated list of trust statements.")
    @PageableAsQueryParam
    public PagedModel<TrustStatementPartnerLinkListItemDto> getPartnerLinks(
        @ParameterObject TrustStatementPartnerLinkFilterDto filters,
        @SortDefault(sort = "updatedAt", direction = Sort.Direction.DESC) @Parameter(
            hidden = true
        ) final Pageable pageable
    ) {
        return new PagedModel<>(this.trustStatementService.getPartnerLinks(filters, pageable));
    }

    @GetMapping("/{trustStatementId}")
    @PreAuthorize("hasAnyRole('" + UserRoles.READER + "', '" + UserRoles.EDITOR + "')")
    @Operation(summary = "IF-012.103 - Get detailed info to a trust statement request.")
    public TrustStatementPartnerLinkDto getSubmission(@PathVariable @Valid @NotNull UUID trustStatementId) {
        return this.trustStatementService.getPartnerLink(trustStatementId);
    }

    @DeleteMapping("/{trustStatementId}")
    @PreAuthorize("hasRole('" + UserRoles.EDITOR + "')")
    @Operation(summary = "IF-012.104 - Deactivates a trust statement.")
    public TrustStatementPartnerLinkDto deactivateSubmission(
        @PathVariable @Valid @NotNull UUID trustStatementId,
        @Valid @NotNull @RequestBody TrustStatementPartnerLinkDeactivationRequestDto request
    ) {
        return this.trustStatementService.deactivateTrustStatement(trustStatementId, request);
    }
}
