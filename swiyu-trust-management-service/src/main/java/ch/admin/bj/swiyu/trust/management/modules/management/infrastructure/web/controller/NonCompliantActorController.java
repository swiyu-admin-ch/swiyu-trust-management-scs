package ch.admin.bj.swiyu.trust.management.modules.management.infrastructure.web.controller;

import static ch.admin.bj.swiyu.trust.management.modules.common.security.SecurityContextSupport.getCurrentUserFullName;

import ch.admin.bj.swiyu.trust.management.modules.common.auth.UserRoles;
import ch.admin.bj.swiyu.trust.management.modules.management.api.NonCompliantActorDto;
import ch.admin.bj.swiyu.trust.management.modules.management.api.NonCompliantActorFilterDto;
import ch.admin.bj.swiyu.trust.management.modules.management.api.NonCompliantActorRequestDto;
import ch.admin.bj.swiyu.trust.management.modules.management.service.NonCompliantActorPublicationService;
import ch.admin.bj.swiyu.trust.management.modules.management.service.NonCompliantActorService;
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
@Tag(name = "NonCompliantActor")
@RestController
@RequestMapping("/api/v1/non-compliant-actors")
@PreAuthorize("isAuthenticated()")
public class NonCompliantActorController {

    private final NonCompliantActorService nonCompliantActorService;
    private final NonCompliantActorPublicationService nonCompliantActorPublicationService;

    @GetMapping("/{nonCompliantActorId}")
    @PreAuthorize("hasAnyRole('" + UserRoles.READER + "', '" + UserRoles.EDITOR + "')")
    public NonCompliantActorDto getNonCompliantActor(@PathVariable @Valid @NotNull UUID nonCompliantActorId) {
        return this.nonCompliantActorService.getNonCompliantActor(nonCompliantActorId);
    }

    @GetMapping
    @PreAuthorize("hasAnyRole('" + UserRoles.READER + "', '" + UserRoles.EDITOR + "')")
    @PageableAsQueryParam
    public PagedModel<NonCompliantActorDto> getNonCompliantActors(
        @ParameterObject @Valid @NotNull NonCompliantActorFilterDto filters,
        @SortDefault(sort = "audit.lastModifiedAt", direction = Sort.Direction.DESC) @Parameter(
            hidden = true
        ) final Pageable pageable
    ) {
        return new PagedModel<>(this.nonCompliantActorService.getNonCompliantActors(filters, pageable));
    }

    @PostMapping
    @PreAuthorize("hasRole('" + UserRoles.EDITOR + "')")
    public ResponseEntity<NonCompliantActorDto> createNonCompliantActor(
        @RequestBody @Valid @NotNull NonCompliantActorRequestDto request
    ) {
        var nonCompliantActor = this.nonCompliantActorService.createNonCompliantActor(
            request,
            getCurrentUserFullName()
        );
        this.nonCompliantActorPublicationService.triggerPublicationAsync();
        return new ResponseEntity<>(nonCompliantActor, HttpStatus.CREATED);
    }

    @DeleteMapping("/{nonCompliantActorId}")
    @PreAuthorize("hasRole('" + UserRoles.EDITOR + "')")
    public void deleteNonCompliantActor(@PathVariable @Valid @NotNull UUID nonCompliantActorId) {
        this.nonCompliantActorService.deleteNonCompliantActor(nonCompliantActorId, getCurrentUserFullName());
        this.nonCompliantActorPublicationService.triggerPublicationAsync();
    }
}
