package ch.admin.bj.swiyu.trust.management.modules.management.api;

import io.swagger.v3.oas.annotations.media.Schema;
import org.springframework.web.bind.annotation.RequestParam;

@Schema(name = "BusinessPartnerViewFilter")
public record BusinessPartnerViewFilterDto(
    @Schema(description = "Matches a part of the partner name or of the partner id, both case insensitive.")
    @RequestParam(required = false)
    String searchText
) {}
