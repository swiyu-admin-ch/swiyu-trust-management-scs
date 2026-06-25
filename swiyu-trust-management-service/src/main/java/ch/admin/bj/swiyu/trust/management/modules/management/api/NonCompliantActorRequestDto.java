package ch.admin.bj.swiyu.trust.management.modules.management.api;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;

@Schema(name = "NonCompliantActorRequest")
public record NonCompliantActorRequestDto(@NotNull String did, @NotNull NonCompliantReasonTextDto reason) {}
