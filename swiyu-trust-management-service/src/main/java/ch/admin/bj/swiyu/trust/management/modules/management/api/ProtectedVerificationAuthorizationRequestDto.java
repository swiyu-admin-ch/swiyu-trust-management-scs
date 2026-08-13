package ch.admin.bj.swiyu.trust.management.modules.management.api;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import java.util.UUID;

@Schema(name = "ProtectedVerificationAuthorizationRequest")
public record ProtectedVerificationAuthorizationRequestDto(
    @NotNull UUID businessPartnerIdentityId,
    @NotNull AuthorizableFieldDto protectedField
) {}
