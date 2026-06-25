package ch.admin.bj.swiyu.trust.management.modules.registry.api;

import io.swagger.v3.oas.annotations.media.Schema;
import java.util.List;

@Schema(name = "NonCompliantActors")
public record NonCompliantActorsDto(List<NonCompliantActorDto> nonCompliantActors) {}
