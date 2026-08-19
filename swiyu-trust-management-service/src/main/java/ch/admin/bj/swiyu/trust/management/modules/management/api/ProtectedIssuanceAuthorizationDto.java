package ch.admin.bj.swiyu.trust.management.modules.management.api;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.util.Map;
import java.util.UUID;

@Schema(name = "ProtectedIssuanceAuthorization")
public record ProtectedIssuanceAuthorizationDto(
    @NotNull UUID id,
    @NotNull UUID businessPartnerIdentityId,
    @NotNull UUID protectedIssuanceEntryId,
    @NotBlank String vct,
    @NotNull Map<@NotBlank String, @NotBlank String> name,
    Map<@NotBlank String, @NotBlank String> reason
) {}
