package ch.admin.bj.swiyu.trust.management.modules.management.infrastructure.web.controller;

import ch.admin.bj.swiyu.trust.management.modules.common.auth.UserRoles;
import ch.admin.bj.swiyu.trust.management.modules.management.api.TrustStatementPartnerLinkDto;
import ch.admin.bj.swiyu.trust.management.modules.management.api.TrustStatementPartnerLinkMetadataV1RequestDto;
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

/**
 * @deprecated use new issuance, identity or verification endpoints instead.
 */
@Deprecated(since = "2.2.0")
@Slf4j
@AllArgsConstructor
@Tag(name = "TrustStatement Metadata", description = "Will be replaced by Trust Statement Identity V1 in the future.")
@RestController
@RequestMapping("/api/v1/trust-statement-partner-links/metadata")
public class TrustStatementPartnerLinkMetadataV1Controller /* NOSONAR */ {

    private final TrustStatementService trustStatementService;

    @PostMapping("")
    @PreAuthorize("hasRole('" + UserRoles.EDITOR + "')")
    @Operation(
        summary = "IF-012.101 - Prepare a new trust statement request. If confirmed it gets also issued and published."
    )
    public ResponseEntity<TrustStatementPartnerLinkDto> issueAndPublishMetadataTrustStatement(
        @Valid @RequestBody TrustStatementPartnerLinkMetadataV1RequestDto request
    ) {
        var statement = this.trustStatementService.issueAndPublishMetadataTrustStatement(request);
        return new ResponseEntity<>(statement, HttpStatus.CREATED);
    }
}
