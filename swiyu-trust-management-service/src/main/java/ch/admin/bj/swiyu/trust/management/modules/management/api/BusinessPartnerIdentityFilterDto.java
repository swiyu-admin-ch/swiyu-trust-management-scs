package ch.admin.bj.swiyu.trust.management.modules.management.api;

import io.swagger.v3.oas.annotations.media.Schema;
import org.springframework.web.bind.annotation.RequestParam;

@Schema(name = "BusinessPartnerIdentityFilter")
public record BusinessPartnerIdentityFilterDto(
    @RequestParam(required = false) String lastModifiedBy,
    @RequestParam(required = false) String createdBy
) {}
