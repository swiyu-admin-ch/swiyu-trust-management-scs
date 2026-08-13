package ch.admin.bj.swiyu.trust.management.modules.management.infrastructure.web.controller;

import static ch.admin.bj.swiyu.trust.management.modules.common.auth.UserRole.Expressions.HAS_ROLE_EDITOR;
import static ch.admin.bj.swiyu.trust.management.modules.common.auth.UserRole.Expressions.HAS_ROLE_EDITOR_OR_READER;

import ch.admin.bj.swiyu.trust.management.modules.management.api.ProtectedVerificationAuthorizationDto;
import ch.admin.bj.swiyu.trust.management.modules.management.api.ProtectedVerificationAuthorizationFilterDto;
import ch.admin.bj.swiyu.trust.management.modules.management.api.ProtectedVerificationAuthorizationRequestDto;
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
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@Slf4j
@AllArgsConstructor
@Tag(name = "ProtectedVerificationAuthorization")
@RestController
@RequestMapping("/ui-api/v1/protected-verification-authorization")
public class ProtectedVerificationAuthorizationController {

    private final BusinessPartnerIdentityService businessPartnerIdentityService;

    @GetMapping("/")
    @PreAuthorize(HAS_ROLE_EDITOR_OR_READER)
    @Operation(summary = "Get paginated list of protected verification authorization")
    @PageableAsQueryParam
    public PagedModel<ProtectedVerificationAuthorizationDto> getProtectedVerificationAuthorizationPaged(
        @ParameterObject ProtectedVerificationAuthorizationFilterDto filters,
        @SortDefault(sort = "updatedAt", direction = Sort.Direction.DESC) @Parameter(
            hidden = true
        ) final Pageable pageable
    ) {
        return new PagedModel<>(
            businessPartnerIdentityService.getProtectedVerificationAuthorizationsPaged(filters, pageable)
        );
    }

    @GetMapping("/{protectedVerificationAuthorizationId}")
    @PreAuthorize(HAS_ROLE_EDITOR_OR_READER)
    @Operation(summary = "Get a protected verification authorization")
    public ProtectedVerificationAuthorizationDto getProtectedVerificationAuthorization(
        @PathVariable @Valid @NotNull UUID protectedVerificationAuthorizationId
    ) {
        return businessPartnerIdentityService.getProtectedVerificationAuthorizationDto(
            protectedVerificationAuthorizationId
        );
    }

    @PostMapping("/")
    @PreAuthorize(HAS_ROLE_EDITOR)
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Create a protected verification authorization")
    public ProtectedVerificationAuthorizationDto createProtectedVerificationAuthorization(
        @RequestBody @Valid @NotNull ProtectedVerificationAuthorizationRequestDto protectedVerificationAuthorizationRequest
    ) {
        return businessPartnerIdentityService.addProtectedVerificationAuthorization(
            protectedVerificationAuthorizationRequest
        );
    }

    @DeleteMapping("/{protectedVerificationAuthorizationId}")
    @PreAuthorize(HAS_ROLE_EDITOR)
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @Operation(summary = "Delete a protected verification authorization")
    public void deleteProtectedVerificationAuthorization(
        @PathVariable @Valid @NotNull UUID protectedVerificationAuthorizationId
    ) {
        businessPartnerIdentityService.removeProtectedVerificationAuthorization(protectedVerificationAuthorizationId);
    }
}
