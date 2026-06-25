package ch.admin.bj.swiyu.trust.management.modules.management.api;

import io.swagger.v3.oas.annotations.media.Schema;
import java.time.Instant;
import java.util.UUID;

@Schema(name = "TrustStatementPartnerLinkListItem")
public record TrustStatementPartnerLinkListItemDto(
    UUID id,
    String subject,
    TrustStatementTypeDto type,
    TrustStatementPartnerLinkStatusDto status,

    // AuditMetadata
    Instant lastModifiedAt,
    String lastModifiedBy,
    Instant createdAt,
    String createdBy
) {}
