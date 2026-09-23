package ch.admin.bj.swiyu.trust.management.modules.management.api;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import java.time.Instant;
import java.util.UUID;

@Schema(name = "NonCompliantActor")
public record NonCompliantActorDto(
    @NotNull UUID id,
    String did,
    UUID businessPartnerId,
    @NotNull Instant flaggedAsNonCompliantAt,
    @NotNull NonCompliantReasonTextDto reason
) {
    public NonCompliantActorDto {
        if ((did == null) == (businessPartnerId == null)) {
            throw new IllegalArgumentException(
                "Validation failed: exactly one of 'did' or 'businessPartnerId' must be set."
            );
        }
    }
}
