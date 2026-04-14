package ch.admin.bj.swiyu.trust.management.modules.management.api;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import java.time.Instant;
import java.util.UUID;

@Schema(name = "NonCompliantActor")
public record NonCompliantActorDto(
    @NotNull UUID id,
    @NotNull String did,
    @NotNull Instant flaggedAsNonCompliantAt,
    @NotNull NonCompliantReasonTextDto reason
) {}
