package ch.admin.bj.swiyu.trust.management.modules.management.domain;

import ch.admin.bj.swiyu.trust.management.modules.common.i18n.ValidLocalizedMap;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Serves as an abstraction layer between the different input channels (TrustOnboardingSubmission, AddDidSubmission)
 * and the statements which express the current identity (IdentityTrustStatementV1,IdentityTrustStatementV2)
 *
 * @param businessPartnerId
 * @param entityName
 * @param isStateActor
 * @param registryIds
 */
@Valid
public record BusinessPartnerIdentity(
    UUID businessPartnerId,
    @ValidLocalizedMap Map<String, @NotBlank String> entityName,
    Boolean isStateActor,
    List<@Valid RegistryId> registryIds
) {
    public record RegistryId(@NotBlank String type, @NotBlank String value) {}
}
