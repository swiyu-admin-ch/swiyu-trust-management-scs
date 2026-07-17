package ch.admin.bj.swiyu.trust.management.modules.management.infrastructure.web.controller;

import static ch.admin.bj.swiyu.trust.management.modules.common.auth.UserRole.Expressions.HAS_ROLE_EDITOR;
import static ch.admin.bj.swiyu.trust.management.modules.common.auth.UserRole.Expressions.HAS_ROLE_EDITOR_OR_READER;
import static ch.admin.bj.swiyu.trust.management.modules.common.security.SecurityContextSupport.getCurrentUserFullName;

import ch.admin.bj.swiyu.trust.management.modules.management.api.ProtectedIssuanceEntryCreateRequestDto;
import ch.admin.bj.swiyu.trust.management.modules.management.api.ProtectedIssuanceEntryDto;
import ch.admin.bj.swiyu.trust.management.modules.management.api.ProtectedIssuanceEntryFilterDto;
import ch.admin.bj.swiyu.trust.management.modules.management.service.ProtectedIssuanceEntryService;
import ch.admin.bj.swiyu.trust.management.modules.management.service.ProtectedIssuanceTrustListStatementPublicationService;
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
@Tag(name = "TrustStatement Issuance")
@RestController
@RequestMapping("/api/v2/protected-issuance-entry")
public class ProtectedIssuanceEntryController {

    private final ProtectedIssuanceEntryService protectedIssuanceEntryService;
    private final ProtectedIssuanceTrustListStatementPublicationService protectedIssuanceTrustListStatementPublicationService;

    @PostMapping("")
    @PreAuthorize(HAS_ROLE_EDITOR)
    @Operation(
        summary = "Add a new vct to the list of protected VCTs in the ecosystem. Also publishes a new Protected Issuance Trust List Statement."
    )
    public ResponseEntity<ProtectedIssuanceEntryDto> addProtectedIssuanceEntry(
        @Valid @RequestBody ProtectedIssuanceEntryCreateRequestDto request
    ) {
        var result = this.protectedIssuanceEntryService.createProtectedIssuanceEntry(request, getCurrentUserFullName());
        protectedIssuanceTrustListStatementPublicationService.triggerPublicationAsync();
        return new ResponseEntity<>(result, HttpStatus.CREATED);
    }

    @DeleteMapping("/{id}")
    @PreAuthorize(HAS_ROLE_EDITOR)
    @Operation(summary = "Delete a protected VCT.")
    public void deleteProtectedIssuanceEntry(@Valid @PathVariable UUID id) {
        this.protectedIssuanceEntryService.deleteProtectedIssuanceEntry(id, getCurrentUserFullName());
        protectedIssuanceTrustListStatementPublicationService.triggerPublicationAsync();
    }

    @GetMapping("/{id}")
    @PreAuthorize(HAS_ROLE_EDITOR_OR_READER)
    @Operation(summary = "Get a protected VCT.")
    public ProtectedIssuanceEntryDto getProtectedIssuanceEntry(@Valid @PathVariable UUID id) {
        return this.protectedIssuanceEntryService.getProtectedIssuanceEntry(id);
    }

    @GetMapping("/")
    @PreAuthorize(HAS_ROLE_EDITOR_OR_READER)
    @Operation(summary = "Get a list of protected VCT.")
    @PageableAsQueryParam
    public PagedModel<ProtectedIssuanceEntryDto> listProtectedIssuanceEntries(
        @ParameterObject @Valid @NotNull ProtectedIssuanceEntryFilterDto filters,
        @SortDefault(sort = "audit.lastModifiedAt", direction = Sort.Direction.DESC) @Parameter(
            hidden = true
        ) final Pageable pageable
    ) {
        return new PagedModel<>(this.protectedIssuanceEntryService.listProtectedIssuanceEntries(filters, pageable));
    }
}
