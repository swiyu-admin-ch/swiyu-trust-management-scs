package ch.admin.bj.swiyu.trust.management.modules.management.api;

import io.swagger.v3.oas.annotations.media.*;
import jakarta.validation.constraints.*;
import java.time.*;
import lombok.*;

@Getter
@EqualsAndHashCode(callSuper = true)
@Schema(name = "TrustStatementPartnerLinkIssuanceV1Request")
@NoArgsConstructor
public class TrustStatementPartnerLinkIssuanceV1RequestDto extends TrustStatementPartnerLinkV1RequestDto {

    @NotNull
    @Schema(
        description = "Identifies the type of a VC that can be issued.",
        example = "https://trust-reg.trust-infra.swiyu.admin.ch/api/v1/vc-schema/test-vc-schema"
    )
    String canIssue;

    public TrustStatementPartnerLinkIssuanceV1RequestDto(
        String subject,
        Instant validFrom,
        Instant validUntil,
        String canIssue
    ) {
        super(subject, validFrom, validUntil);
        this.canIssue = canIssue;
    }
}
