package ch.admin.bj.swiyu.trust.management.modules.management.api;

import io.swagger.v3.oas.annotations.media.Schema;
import java.util.Map;
import java.util.UUID;

@Schema(name = "ZasData", description = "Live snapshot of ZAS (Zentrale Ausgleichsstelle) SBN/USN data for a business")
public record ZasDataDto(
    UUID businessId,
    OrganisationDto organisation,
    String statusCode,
    Map<String, String> statusTranslation,
    Long commissionedOrganisationId,
    String activityDomainCode,
    Map<String, String> activityDomainTranslation
) {
    @Schema(name = "ZasDataOrganisation")
    public record OrganisationDto(
        String canton,
        String ide,
        String name,
        String phoneNumber,
        String streetName,
        String streetNumber,
        String poBox,
        String npa,
        String locality
    ) {}
}
