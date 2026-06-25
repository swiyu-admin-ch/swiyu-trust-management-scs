package ch.admin.bj.swiyu.trust.management.modules.management.api;

import io.swagger.v3.oas.annotations.media.Schema;
import org.springframework.web.bind.annotation.RequestParam;

@Schema(name = "TrustStatementPartnerLinkFilter")
public record TrustStatementPartnerLinkFilterDto(
    @RequestParam(required = false) String subject,
    @RequestParam(required = false) TrustStatementTypeDto type,
    @RequestParam(required = false) TrustStatementPartnerLinkStatusDto status,

    @RequestParam(required = false) String lastModifiedBy,
    @RequestParam(required = false) String createdBy
) {}
