package ch.admin.bj.swiyu.trust.management.modules.management.api;

import io.swagger.v3.oas.annotations.media.Schema;
import java.util.UUID;
import org.springframework.web.bind.annotation.RequestParam;

@Schema(name = "ProtectedVerificationAuthorizationFilter")
public record ProtectedIssuanceAuthorizationFilterDto(
    @RequestParam(required = false) UUID businessPartnerIdentityId,
    @RequestParam(required = false) String lastModifiedBy,
    @RequestParam(required = false) String createdBy
) {}
