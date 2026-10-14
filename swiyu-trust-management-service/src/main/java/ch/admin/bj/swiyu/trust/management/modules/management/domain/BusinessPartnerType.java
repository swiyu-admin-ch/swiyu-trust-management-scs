package ch.admin.bj.swiyu.trust.management.modules.management.domain;

/**
 * The partner types as the core business service defines them, kept in sync with its
 * {@code BusinessPartnerTypeDto} by name.
 *
 * <p>Its own domain enum rather than the one in {@code modules.management.api}: the architecture test lets only
 * service and infrastructure reach into {@code api}, and an entity lives in the domain.
 */
public enum BusinessPartnerType {
    GOVERNMENTAL_INSTITUTION,
    BUSINESS,
    INDIVIDUAL,
}
