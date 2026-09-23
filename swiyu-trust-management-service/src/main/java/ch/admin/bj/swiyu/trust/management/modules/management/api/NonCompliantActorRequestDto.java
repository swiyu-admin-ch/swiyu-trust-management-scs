package ch.admin.bj.swiyu.trust.management.modules.management.api;

import io.swagger.v3.oas.annotations.media.Schema;
import java.util.UUID;

@Schema(name = "NonCompliantActorRequest")
public record NonCompliantActorRequestDto(String did, UUID businessPartnerId, NonCompliantReasonTextDto reason) {
    public NonCompliantActorRequestDto {
        if ((did == null) == (businessPartnerId == null)) {
            throw new IllegalArgumentException(
                "Validation failed: exactly one of 'did' or 'businessPartnerId' must be set."
            );
        }
    }
}
