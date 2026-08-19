package ch.admin.bj.swiyu.trust.management.modules.management.infrastructure.web.controller;

import static ch.admin.bj.swiyu.trust.management.modules.common.auth.UserRole.Expressions.HAS_ROLE_EDITOR;
import static ch.admin.bj.swiyu.trust.management.modules.common.auth.UserRole.Expressions.HAS_ROLE_EDITOR_OR_READER;
import static ch.admin.bj.swiyu.trust.management.modules.common.security.SecurityContextSupport.getCurrentUserFullName;

import ch.admin.bj.swiyu.trust.management.modules.management.api.ProtectedIssuanceAuthorizationCreateRequestDto;
import ch.admin.bj.swiyu.trust.management.modules.management.api.ProtectedIssuanceAuthorizationDto;
import ch.admin.bj.swiyu.trust.management.modules.management.api.ProtectedIssuanceAuthorizationFilterDto;
import ch.admin.bj.swiyu.trust.management.modules.management.service.ProtectedIssuanceService;
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
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@Slf4j
@AllArgsConstructor
@Tag(name = "ProtectedIssuanceAuthorization")
@RestController
@RequestMapping("/ui-api/v1/protected-issuance-authorization")
public class ProtectedIssuanceAuthorizationController {

    private final ProtectedIssuanceService protectedIssuanceService;

    @GetMapping("/")
    @PreAuthorize(HAS_ROLE_EDITOR_OR_READER)
    @Operation(summary = "List all ProtectedIssuanceAuthorizations for a BusinessPartnerIdentity.")
    @PageableAsQueryParam
    public PagedModel<ProtectedIssuanceAuthorizationDto> listProtectedIssuanceAuthorization(
        @ParameterObject ProtectedIssuanceAuthorizationFilterDto filters,
        @SortDefault(sort = "updatedAt", direction = Sort.Direction.DESC) @Parameter(
            hidden = true
        ) final Pageable pageable
    ) {
        return new PagedModel<>(protectedIssuanceService.getProtectedIssuanceAuthorizationsPaged(filters, pageable));
    }

    @GetMapping("/{id}")
    @PreAuthorize(HAS_ROLE_EDITOR_OR_READER)
    @Operation(summary = "Get a single ProtectedIssuanceAuthorization by id.")
    public ProtectedIssuanceAuthorizationDto getProtectedIssuanceAuthorization(@PathVariable @NotNull UUID id) {
        return protectedIssuanceService.getAuthorization(id);
    }

    @PostMapping("/")
    @PreAuthorize(HAS_ROLE_EDITOR)
    @Operation(
        summary = "Create a new ProtectedIssuanceAuthorization, linking a BusinessPartnerIdentity to a ProtectedIssuanceEntry."
    )
    public ResponseEntity<ProtectedIssuanceAuthorizationDto> addProtectedIssuanceAuthorization(
        @Valid @RequestBody ProtectedIssuanceAuthorizationCreateRequestDto request
    ) {
        var result = protectedIssuanceService.addAuthorization(request, getCurrentUserFullName());
        return new ResponseEntity<>(result, HttpStatus.CREATED);
    }

    @DeleteMapping("/{id}")
    @PreAuthorize(HAS_ROLE_EDITOR)
    @Operation(summary = "Delete a ProtectedIssuanceAuthorization and revoke its associated piaTS.")
    public void removeProtectedIssuanceAuthorization(@PathVariable @NotNull UUID id) {
        protectedIssuanceService.removeAuthorization(id, getCurrentUserFullName());
    }
}
