package ch.admin.bj.swiyu.trust.management.modules.ui.api;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(name = "PartnerType", enumAsRef = true)
public enum BusinessPartnerTypeDto {
    GOVERNMENTAL_INSTITUTION,
    BUSINESS,
    INDIVIDUAL,
    UNKNOWN, // Temporary type as part of migration for existing legacy partners
}
