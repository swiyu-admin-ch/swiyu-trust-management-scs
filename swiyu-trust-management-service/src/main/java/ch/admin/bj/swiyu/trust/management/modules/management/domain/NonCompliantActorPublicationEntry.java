package ch.admin.bj.swiyu.trust.management.modules.management.domain;

import java.time.Instant;

/**
 * A non-compliant actor prepared for publication, always carrying a concrete DID. Actors that were
 * flagged by their business partner id are resolved against CBS into one entry per DID.
 */
public record NonCompliantActorPublicationEntry(
    String did,
    Instant flaggedAsNonCompliantAt,
    NonCompliantReasonText reason
) {}
