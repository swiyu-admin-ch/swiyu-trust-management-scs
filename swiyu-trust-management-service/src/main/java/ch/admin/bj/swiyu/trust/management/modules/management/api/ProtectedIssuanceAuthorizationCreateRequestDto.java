package ch.admin.bj.swiyu.trust.management.modules.management.api;

import ch.admin.bj.swiyu.trust.management.modules.common.i18n.ValidLocalizedMap;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.util.Map;
import java.util.UUID;

@Schema(name = "ProtectedIssuanceAuthorizationCreateRequest")
public record ProtectedIssuanceAuthorizationCreateRequestDto(
    @NotNull UUID protectedIssuanceEntryId,
    @NotNull UUID businessPartnerIdentityId,

    @Valid @ValidLocalizedMap Map<@NotBlank String, @NotBlank String> reason
) {}
