package ch.admin.bj.swiyu.trust.management.modules.management.api;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(name = "IssuerVcStatus", enumAsRef = true)
public enum IssuerVcStatusDto {
    UNKNOWN,
    OTHER,
    VALID,
    INVALID,
}
