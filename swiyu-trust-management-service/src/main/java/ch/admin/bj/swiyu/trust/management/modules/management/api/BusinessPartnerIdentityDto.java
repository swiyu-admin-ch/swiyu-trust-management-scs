package ch.admin.bj.swiyu.trust.management.modules.management.api;

import io.swagger.v3.oas.annotations.media.Schema;
import java.time.Instant;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

@Schema(name = "BusinessPartnerIdentity")
public record BusinessPartnerIdentityDto(
    UUID id,
    Map<String, String> entityName,
    Instant lastActivated,
    String uid,
    Boolean isRegisteredInCommercialRegister,
    String correspondingLanguage,
    BusinessPartnerIdentityStatusDto status,
    Boolean isStateActor,
    Set<String> trustedIdentifier, // DIDs to represent the BPI
    Instant validUntil,
    Instant lastIssuanceAt,
    Long version,

    // AuditMetadata
    Instant lastModifiedAt,
    String lastModifiedBy,
    Instant createdAt,
    String createdBy
) {}
