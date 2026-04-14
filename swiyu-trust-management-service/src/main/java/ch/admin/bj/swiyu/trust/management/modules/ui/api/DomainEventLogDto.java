package ch.admin.bj.swiyu.trust.management.modules.ui.api;

import io.swagger.v3.oas.annotations.media.Schema;
import java.time.Instant;

@Schema(name = "DomainEventLog", enumAsRef = true)
public record DomainEventLogDto(
    Instant triggeredAt,
    String triggeredBy,
    DomainEventTypeDto eventType,
    String partnerNote,
    String internalNote
) {}
