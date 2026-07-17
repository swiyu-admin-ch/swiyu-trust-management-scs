package ch.admin.bj.swiyu.trust.management.modules.management.infrastructure.web.controller;

import static ch.admin.bj.swiyu.trust.management.modules.common.auth.UserRole.Expressions.HAS_ROLE_EDITOR;

import ch.admin.bj.swiyu.trust.management.modules.management.api.ProtectedIssuanceAuthorizationV2RequestDto;
import ch.admin.bj.swiyu.trust.management.modules.management.api.TrustStatementPartnerLinkDto;
import ch.admin.bj.swiyu.trust.management.modules.management.service.TrustStatementService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Slf4j
@AllArgsConstructor
@Tag(name = "TrustStatement Issuance Authorization")
@RestController
@RequestMapping("/api/v2/trust-statement-partner-links/issuance")
public class TrustStatementPartnerLinkProtectedIssuanceAuthorizationV2Controller {

    private final TrustStatementService trustStatementService;

    @PostMapping("")
    @PreAuthorize(HAS_ROLE_EDITOR)
    @Operation(summary = "Prepare a new trust statement request. If confirmed it gets also issued and published.")
    public ResponseEntity<TrustStatementPartnerLinkDto> issueAndPublishProtectedIssuanceAuthorizationV2Statement(
        @Valid @RequestBody ProtectedIssuanceAuthorizationV2RequestDto request
    ) {
        var statement = this.trustStatementService.issueAndPublishProtectedIssuanceAuthorizationV2TrustStatement(
            request
        );
        return new ResponseEntity<>(statement, HttpStatus.CREATED);
    }
}
