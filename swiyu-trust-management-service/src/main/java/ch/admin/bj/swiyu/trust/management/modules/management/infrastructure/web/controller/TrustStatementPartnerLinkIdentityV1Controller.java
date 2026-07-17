package ch.admin.bj.swiyu.trust.management.modules.management.infrastructure.web.controller;

import static ch.admin.bj.swiyu.trust.management.modules.common.auth.UserRole.Expressions.HAS_ROLE_EDITOR;

import ch.admin.bj.swiyu.trust.management.modules.management.api.IdentityV1RequestDto;
import ch.admin.bj.swiyu.trust.management.modules.management.api.TrustStatementPartnerLinkDto;
import ch.admin.bj.swiyu.trust.management.modules.management.service.TrustStatementService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.util.UUID;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@Slf4j
@AllArgsConstructor
@Tag(name = "TrustStatement Identity")
@RestController
@RequestMapping("/api/v1/trust-statement-partner-links/identity")
public class TrustStatementPartnerLinkIdentityV1Controller {

    private final TrustStatementService trustStatementService;

    @PostMapping("")
    @PreAuthorize(HAS_ROLE_EDITOR)
    @Operation(summary = "Prepare a new trust statement request. If confirmed it gets also issued and published.")
    public ResponseEntity<TrustStatementPartnerLinkDto> issueAndPublishIdentityTrustStatement(
        @RequestParam(required = false) @Parameter(
            description = "UUID of the partner. Currently still optional since difficult to know when onboarding trust statements."
        ) UUID partnerId,
        @Valid @RequestBody IdentityV1RequestDto request
    ) {
        var statement = this.trustStatementService.issueAndPublishIdentityV1TrustStatement(partnerId, request);
        return new ResponseEntity<>(statement, HttpStatus.CREATED);
    }
}
