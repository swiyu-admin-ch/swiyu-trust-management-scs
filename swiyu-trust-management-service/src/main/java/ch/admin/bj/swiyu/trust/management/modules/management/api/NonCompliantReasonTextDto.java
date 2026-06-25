package ch.admin.bj.swiyu.trust.management.modules.management.api;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(name = "NonCompliantReasonText")
public record NonCompliantReasonTextDto(
    String reasonDe,
    String reasonFr,
    String reasonIt,
    String reasonEn,
    String reasonRm
) {}
