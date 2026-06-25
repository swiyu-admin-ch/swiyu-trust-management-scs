package ch.admin.bj.swiyu.trust.management.modules.registry.api;

import io.swagger.v3.oas.annotations.media.Schema;
import java.time.Instant;
import java.util.Map;

@Schema(name = "NonCompliantActor")
public record NonCompliantActorDto(String did, Instant flaggedAsNonCompliantAt, Map<String, String> reason) {}
