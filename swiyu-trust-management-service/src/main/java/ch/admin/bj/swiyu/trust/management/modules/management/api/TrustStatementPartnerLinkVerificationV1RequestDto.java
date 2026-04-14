package ch.admin.bj.swiyu.trust.management.modules.management.api;

import io.swagger.v3.oas.annotations.media.*;
import jakarta.validation.constraints.*;
import java.time.*;
import lombok.*;

@Getter
@EqualsAndHashCode(callSuper = true)
@Schema(name = "TrustStatementPartnerLinkVerificationV1Request")
@NoArgsConstructor
public class TrustStatementPartnerLinkVerificationV1RequestDto extends TrustStatementPartnerLinkV1RequestDto {

    @NotNull
    @Schema(
        description = "Identifies the type of a VC that can be verified.",
        example = "https://trust-reg.trust-infra.swiyu.admin.ch/api/v1/vc-schema/test-vc-schema"
    )
    String canVerify;

    public TrustStatementPartnerLinkVerificationV1RequestDto(
        String subject,
        Instant validFrom,
        Instant validUntil,
        String canVerify
    ) {
        super(subject, validFrom, validUntil);
        this.canVerify = canVerify;
    }
}
