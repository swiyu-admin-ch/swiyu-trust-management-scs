package ch.admin.bj.swiyu.trust.management.modules.management.api;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import java.time.Instant;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@EqualsAndHashCode(callSuper = true)
@Schema(name = "VerificationV1Request")
@NoArgsConstructor
public class VerificationV1RequestDto extends V1RequestDto {

    @NotNull
    @Schema(
        description = "Identifies the type of a VC that can be verified.",
        example = "https://trust-reg.trust-infra.swiyu.admin.ch/api/v1/vc-schema/test-vc-schema"
    )
    String canVerify;

    public VerificationV1RequestDto(String subject, Instant validFrom, Instant validUntil, String canVerify) {
        super(subject, validFrom, validUntil);
        this.canVerify = canVerify;
    }
}
