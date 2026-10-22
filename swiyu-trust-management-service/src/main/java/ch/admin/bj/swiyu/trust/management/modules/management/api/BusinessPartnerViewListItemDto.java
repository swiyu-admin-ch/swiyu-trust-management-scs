package ch.admin.bj.swiyu.trust.management.modules.management.api;

import io.swagger.v3.oas.annotations.media.Schema;
import java.time.Instant;
import java.util.UUID;

@Schema(name = "BusinessPartnerViewListItem")
public record BusinessPartnerViewListItemDto(
    UUID businessPartnerId,
    String name,
    @Schema(description = "Empty for a partner that is registered but has never been onboarded.")
    BusinessPartnerIdentityStatusDto status,
    @Schema(description = "When the current identity expires.") Instant validUntil
) {}
