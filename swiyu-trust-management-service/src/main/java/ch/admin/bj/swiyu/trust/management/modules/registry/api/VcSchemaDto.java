package ch.admin.bj.swiyu.trust.management.modules.registry.api;

import io.swagger.v3.oas.annotations.media.Schema;
import java.util.UUID;

@Schema(name = "VcSchema", enumAsRef = true)
public record VcSchemaDto(UUID id, String file, String path, VcSchemaStatusDto status) {}
