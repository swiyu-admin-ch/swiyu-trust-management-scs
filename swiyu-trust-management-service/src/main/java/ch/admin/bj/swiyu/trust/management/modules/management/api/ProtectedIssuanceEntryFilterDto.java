package ch.admin.bj.swiyu.trust.management.modules.management.api;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(name = "ProtectedIssuanceEntryFilter")
public record ProtectedIssuanceEntryFilterDto(String vct) {}
