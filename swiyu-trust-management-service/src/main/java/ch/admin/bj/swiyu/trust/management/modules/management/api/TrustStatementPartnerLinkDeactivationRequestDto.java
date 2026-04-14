package ch.admin.bj.swiyu.trust.management.modules.management.api;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

@Data
@Schema(name = "TrustStatementPartnerLinkDeactivationRequest")
public class TrustStatementPartnerLinkDeactivationRequestDto {

    String reason;
}
