package ch.admin.bj.swiyu.trust.management.modules.management.service;

import static ch.admin.bj.swiyu.trust.management.modules.common.i18n.LocalizedMapConstants.DEFAULT_VALUE_KEY;

import ch.admin.bj.swiyu.messagetype.ti.BusinessPartnerIdentityStatus;
import ch.admin.bj.swiyu.trust.client.core.business.internal.model.BusinessPartnerTypeDto;
import ch.admin.bj.swiyu.trust.client.core.business.internal.model.TrustOnboardingSubmissionDto;
import ch.admin.bj.swiyu.trust.management.modules.management.api.*;
import ch.admin.bj.swiyu.trust.management.modules.management.domain.BusinessPartnerIdentity;
import ch.admin.bj.swiyu.trust.management.modules.management.domain.ProtectedVerificationAuthorization;
import ch.admin.bj.swiyu.trust.management.modules.management.domain.ProtectedVerificationField;
import ch.admin.bj.swiyu.trust.management.modules.management.domain.TrustStatementPartnerLink;
import ch.admin.bj.swiyu.trust.management.modules.management.domain.details.IdentityV1Details;
import ch.admin.bj.swiyu.trust.management.modules.management.domain.details.IdentityV2Details;
import ch.admin.bj.swiyu.trust.management.modules.management.domain.details.TrustStatementDetails;
import jakarta.validation.constraints.NotNull;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import lombok.experimental.UtilityClass;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;

@UtilityClass
public class BusinessPartnerIdentityMapper {

    private final List<String> businessPartnerIdentityAllowedSortFields = List.of(
        "createdAt",
        "createdBy",
        "updatedAt",
        "updatedBy"
    );

    public static BusinessPartnerIdentityDto toBusinessPartnerIdentityDto(
        BusinessPartnerIdentity businessPartnerIdentity
    ) {
        return new BusinessPartnerIdentityDto(
            businessPartnerIdentity.getId(),
            Map.copyOf(businessPartnerIdentity.getEntityName()),
            businessPartnerIdentity.getLastActivated(),
            businessPartnerIdentity.getUid(),
            businessPartnerIdentity.getIsRegisteredInCommercialRegister(),
            businessPartnerIdentity.getCorrespondingLanguage(),
            toBusinessPartnerIdentityStatusDto(businessPartnerIdentity.getStatus()),
            businessPartnerIdentity.getIsStateActor(),
            businessPartnerIdentity.getTrustedIdentifier(),
            businessPartnerIdentity.getValidUntil(),
            businessPartnerIdentity.getLastIssuanceAt(),
            businessPartnerIdentity.getVersion(),
            businessPartnerIdentity.getAudit().getLastModifiedAt(),
            businessPartnerIdentity.getAudit().getLastModifiedBy(),
            businessPartnerIdentity.getAudit().getCreatedAt(),
            businessPartnerIdentity.getAudit().getCreatedBy()
        );
    }

    // must fully be adapted in EID-6609
    public static BusinessPartnerIdentity toBusinessPartnerIdentity(TrustOnboardingSubmissionDto submission) {
        return new BusinessPartnerIdentity(
            submission.getPartnerId(),
            Map.copyOf(submission.getName()),
            Instant.now(), // must be adapted in EID-6609
            submission.getRegistryIds().get("UID") == null ? null : submission.getRegistryIds().get("UID"),
            submission.getIsRegisteredInCommercialRegister() != null &&
                submission.getIsRegisteredInCommercialRegister(),
            submission.getCorrespondingLanguage() == null ? null : submission.getCorrespondingLanguage().toString(),
            BusinessPartnerIdentityStatus.ACTIVE,
            submission.getBusinessPartnerType() == BusinessPartnerTypeDto.GOVERNMENTAL_INSTITUTION,
            Instant.now().atZone(ZoneOffset.UTC).plusMonths(6).toInstant(), // to be adapted to the config in EID-6609
            Instant.now()
        );
    }

    // must be fully adapted in EID-6609
    public static BusinessPartnerIdentity toBusinessPartnerIdentity(TrustStatementPartnerLink partnerLink) {
        return switch (partnerLink.getType()) {
            case TRUST_STATEMENT_IDENTITY_V1 -> {
                var details = (IdentityV1Details) partnerLink.getDetails();
                var uid = details
                    .getRegistryIds()
                    .stream()
                    .filter(registryId -> "UID".equals(registryId.type()))
                    .map(IdentityV1Details.RegistryId::value)
                    .findFirst()
                    .orElse(null);
                yield new BusinessPartnerIdentity(
                    partnerLink.getPartnerId(),
                    toLocalizedEntityName(details),
                    Instant.now(), // must be adapted in EID-6609
                    uid,
                    false,
                    "de-DE",
                    BusinessPartnerIdentityStatus.ACTIVE,
                    details.getIsStateActor(),
                    partnerLink.getValidUntil(),
                    Instant.now()
                );
            }
            case TRUST_STATEMENT_IDENTITY_V2 -> {
                var details = (IdentityV2Details) partnerLink.getDetails();
                var uid = details
                    .getRegistryIds()
                    .stream()
                    .filter(registryId -> "UID".equals(registryId.type()))
                    .map(IdentityV2Details.RegistryId::value)
                    .findFirst()
                    .orElse(null);
                yield new BusinessPartnerIdentity(
                    partnerLink.getPartnerId(),
                    toLocalizedEntityName(details),
                    Instant.now(), // must be adapted in EID-6609
                    uid,
                    false,
                    "de-DE",
                    BusinessPartnerIdentityStatus.ACTIVE,
                    details.getIsStateActor(),
                    partnerLink.getValidUntil(),
                    Instant.now()
                );
            }
            default -> throw new IllegalArgumentException(
                "Cannot get business partner identity from partner link type: " + partnerLink.getType()
            );
        };
    }

    public static Map<String, String> toLocalizedEntityName(TrustStatementDetails details) {
        var localized = new LinkedHashMap<String, String>();
        switch (details) {
            case IdentityV1Details identity -> identity
                .getEntityName()
                .forEach((language, text) -> putLocalizedEntry(localized, language, text));
            case IdentityV2Details identity -> identity
                .getEntityName()
                .forEach((language, text) -> putLocalizedEntry(localized, language, text));
            default -> throw new IllegalArgumentException(
                "Cannot derive entity name from trust statement details: " + details.getClass().getSimpleName()
            );
        }
        if (!localized.containsKey(DEFAULT_VALUE_KEY)) {
            localized
                .entrySet()
                .stream()
                .min(Map.Entry.comparingByKey())
                .ifPresent(entry -> localized.put(DEFAULT_VALUE_KEY, entry.getValue()));
        }
        return Map.copyOf(localized);
    }

    public static @NotNull IdentityV2RequestDto toTrustStatementPartnerLinkIdentityV2RequestDto(
        String did,
        BusinessPartnerIdentity businessPartnerIdentity
    ) {
        return new IdentityV2RequestDto(
            businessPartnerIdentity.getId(),
            did,
            Instant.now(),
            Instant.now().plus(Duration.ofDays(365)),
            businessPartnerIdentity.getEntityName(),
            businessPartnerIdentity.getIsStateActor(),
            toRegistryIdDtoV2List(businessPartnerIdentity.getUid())
        );
    }

    public static @NotNull IdentityV1RequestDto toTrustStatementPartnerLinkIdentityV1RequestDto(
        String did,
        BusinessPartnerIdentity businessPartnerIdentity
    ) {
        return new IdentityV1RequestDto(
            did,
            Instant.now(),
            Instant.now().plus(Duration.ofDays(365)),
            businessPartnerIdentity.getEntityName(),
            businessPartnerIdentity.getIsStateActor(),
            toRegistryIdDtoV1List(businessPartnerIdentity.getUid())
        );
    }

    public static List<IdentityV1RequestDto.RegistryIdDto> toRegistryIdDtoV1List(String uid) {
        if (uid == null) {
            return List.of();
        }
        return List.of(new IdentityV1RequestDto.RegistryIdDto("UID", uid));
    }

    public static List<IdentityV2RequestDto.RegistryIdDto> toRegistryIdDtoV2List(String uid) {
        if (uid == null) {
            return List.of();
        }
        return List.of(new IdentityV2RequestDto.RegistryIdDto("UID", uid));
    }

    public static Pageable mapBusinessPartnerIdentityPageableWithValidSortProperties(Pageable pageable) {
        // filter Pageable for invalid sort fields to not fail them silently
        pageable
            .getSort()
            .stream()
            .filter(c -> !businessPartnerIdentityAllowedSortFields.contains(c.getProperty()))
            .findFirst()
            .ifPresent(c -> {
                throw new IllegalArgumentException("Invalid pagination parameters");
            });

        // Map Pageable fields to actual DB entities
        var sort = pageable
            .getSort()
            .stream()
            .map(order -> {
                String property = order.getProperty();
                return switch (property) {
                    case "createdAt" -> new Sort.Order(order.getDirection(), "audit.createdAt");
                    case "createdBy" -> new Sort.Order(order.getDirection(), "audit.createdBy");
                    case "updatedAt" -> new Sort.Order(order.getDirection(), "audit.lastModifiedAt");
                    case "updatedBy" -> new Sort.Order(order.getDirection(), "audit.lastModifiedBy");
                    default -> order;
                };
            })
            .toList();
        if (pageable.isUnpaged()) {
            return Pageable.unpaged(Sort.by(sort));
        }
        // Copy Pageable details over as a Pageable is immutable
        return PageRequest.of(pageable.getPageNumber(), pageable.getPageSize(), Sort.by(sort));
    }

    public static ProtectedVerificationField toProtectedVerificationField(
        @NotNull AuthorizableFieldDto authorizableFieldDto
    ) {
        return switch (authorizableFieldDto) {
            case AHV_NUMBER -> ProtectedVerificationField.AHV_NUMBER;
        };
    }

    public static ProtectedVerificationAuthorizationDto toProtectedVerificationAuthorizationDto(
        ProtectedVerificationAuthorization src
    ) {
        return new ProtectedVerificationAuthorizationDto(
            src.getId(),
            src.getBusinessPartnerIdentityId(),
            toProtectedVerificationFieldDto(src.getProtectedVerificationField()),
            src.getAudit().getLastModifiedAt(),
            src.getAudit().getLastModifiedBy(),
            src.getAudit().getCreatedAt(),
            src.getAudit().getCreatedBy()
        );
    }

    public static AuthorizableFieldDto toProtectedVerificationFieldDto(
        ProtectedVerificationField protectedVerificationField
    ) {
        return switch (protectedVerificationField) {
            case AHV_NUMBER -> AuthorizableFieldDto.AHV_NUMBER;
        };
    }

    public static ProtectedVerificationAuthorizationV2RequestDto.AuthorizableFieldDto toProtectedVerificationAuthorizationV2AuthorizedFieldDto(
        ProtectedVerificationField protectedVerificationField
    ) {
        return switch (protectedVerificationField) {
            case AHV_NUMBER -> ProtectedVerificationAuthorizationV2RequestDto.AuthorizableFieldDto.AHV_NUMBER;
        };
    }

    private static void putLocalizedEntry(Map<String, String> target, String languageCode, String text) {
        if (text == null || text.isBlank()) {
            return;
        }
        target.put(languageCode, text);
    }

    private static BusinessPartnerIdentityStatusDto toBusinessPartnerIdentityStatusDto(
        BusinessPartnerIdentityStatus status
    ) {
        return switch (status) {
            case ACTIVE -> BusinessPartnerIdentityStatusDto.ACTIVE;
            case DEACTIVATED -> BusinessPartnerIdentityStatusDto.DEACTIVATED;
        };
    }
}
