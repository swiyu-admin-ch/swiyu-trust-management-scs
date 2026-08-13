package ch.admin.bj.swiyu.trust.management.modules.management.api;

import io.swagger.v3.oas.annotations.media.Schema;
import java.time.Instant;
import java.util.UUID;

@Schema(name = "ProtectedVerificationAuthorization")
public record ProtectedVerificationAuthorizationDto(
    UUID id,
    UUID businessPartnerIdentityId,
    AuthorizableFieldDto authorizableField,

    // AuditMetadata
    Instant lastModifiedAt,
    String lastModifiedBy,
    Instant createdAt,
    String createdBy
) {}
