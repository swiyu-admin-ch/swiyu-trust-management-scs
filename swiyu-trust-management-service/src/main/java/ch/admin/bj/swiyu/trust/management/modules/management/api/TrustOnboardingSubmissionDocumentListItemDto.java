package ch.admin.bj.swiyu.trust.management.modules.management.api;

import java.time.Instant;
import java.util.UUID;

public record TrustOnboardingSubmissionDocumentListItemDto(
    UUID id,
    String name,
    String mediaType,
    String type,
    UUID owningBusinessPartner,
    Instant createdAt,
    Instant updatedAt,
    Instant submittedAt,
    UUID trustOnboardingSubmissionId
) {}
