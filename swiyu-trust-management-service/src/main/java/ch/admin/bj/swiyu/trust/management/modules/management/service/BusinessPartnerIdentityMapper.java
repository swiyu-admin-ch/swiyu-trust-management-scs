package ch.admin.bj.swiyu.trust.management.modules.management.service;

import static ch.admin.bj.swiyu.trust.management.modules.common.i18n.LocalizedMapConstants.DEFAULT_VALUE_KEY;

import ch.admin.bj.swiyu.trust.client.core.business.internal.model.BusinessPartnerTypeDto;
import ch.admin.bj.swiyu.trust.client.core.business.internal.model.TrustOnboardingSubmissionDto;
import ch.admin.bj.swiyu.trust.management.modules.management.api.IdentityV1RequestDto;
import ch.admin.bj.swiyu.trust.management.modules.management.api.IdentityV2RequestDto;
import ch.admin.bj.swiyu.trust.management.modules.management.domain.BusinessPartnerIdentity;
import ch.admin.bj.swiyu.trust.management.modules.management.domain.TrustStatementPartnerLink;
import ch.admin.bj.swiyu.trust.management.modules.management.domain.details.IdentityV1Details;
import ch.admin.bj.swiyu.trust.management.modules.management.domain.details.IdentityV2Details;
import ch.admin.bj.swiyu.trust.management.modules.management.domain.details.TrustStatementDetails;
import java.time.Duration;
import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import lombok.experimental.UtilityClass;
import org.jetbrains.annotations.NotNull;

@UtilityClass
public class BusinessPartnerIdentityMapper {

    public static BusinessPartnerIdentity toBusinessPartnerIdentity(TrustOnboardingSubmissionDto submission) {
        return new BusinessPartnerIdentity(
            submission.getPartnerId(),
            Map.copyOf(submission.getName()),
            submission.getBusinessPartnerType() == BusinessPartnerTypeDto.GOVERNMENTAL_INSTITUTION,
            submission
                .getRegistryIds()
                .entrySet()
                .stream()
                .map(e -> new BusinessPartnerIdentity.RegistryId(e.getKey(), e.getValue()))
                .toList()
        );
    }

    public static BusinessPartnerIdentity toBusinessPartnerIdentity(TrustStatementPartnerLink partnerLink) {
        return switch (partnerLink.getType()) {
            case TRUST_STATEMENT_IDENTITY_V1 -> {
                var details = (IdentityV1Details) partnerLink.getDetails();
                yield new BusinessPartnerIdentity(
                    partnerLink.getPartnerId(),
                    toLocalizedEntityName(details),
                    details.getIsStateActor(),
                    toRegistryIdsFromIdentityV1Details(details.getRegistryIds())
                );
            }
            case TRUST_STATEMENT_IDENTITY_V2 -> {
                var details = (IdentityV2Details) partnerLink.getDetails();
                yield new BusinessPartnerIdentity(
                    partnerLink.getPartnerId(),
                    toLocalizedEntityName(details),
                    details.getIsStateActor(),
                    toRegistryIdsFromIdentityV2Details(details.getRegistryIds())
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
                .forEach((language, text) -> putLocalizedEntry(localized, language.getJsonValue(), text));
            case IdentityV2Details identity -> identity
                .getEntityName()
                .forEach((language, text) -> putLocalizedEntry(localized, language.getLanguageCode(), text));
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
            businessPartnerIdentity.businessPartnerId(),
            did,
            Instant.now(),
            Instant.now().plus(Duration.ofDays(365)),
            businessPartnerIdentity.entityName(),
            businessPartnerIdentity.isStateActor(),
            toRequestRegistryIdsV2(businessPartnerIdentity.registryIds())
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
            businessPartnerIdentity.entityName(),
            businessPartnerIdentity.isStateActor(),
            toRequestRegistryIdsV1(businessPartnerIdentity.registryIds())
        );
    }

    public static List<IdentityV1RequestDto.RegistryIdDto> toRequestRegistryIdsV1(
        List<BusinessPartnerIdentity.RegistryId> source
    ) {
        if (source == null) {
            return List.of();
        }
        return source
            .stream()
            .map(r -> new IdentityV1RequestDto.RegistryIdDto(r.type(), r.value()))
            .toList();
    }

    public static List<IdentityV2RequestDto.RegistryIdDto> toRequestRegistryIdsV2(
        List<BusinessPartnerIdentity.RegistryId> source
    ) {
        if (source == null) {
            return List.of();
        }
        return source
            .stream()
            .map(r -> new IdentityV2RequestDto.RegistryIdDto(r.type(), r.value()))
            .toList();
    }

    private static void putLocalizedEntry(Map<String, String> target, String languageCode, String text) {
        if (text == null || text.isBlank()) {
            return;
        }
        // IdentityV1/V2 details store the "default" translation under the empty language code.
        target.put(languageCode.isEmpty() ? DEFAULT_VALUE_KEY : languageCode, text);
    }

    private static List<BusinessPartnerIdentity.RegistryId> toRegistryIdsFromIdentityV1Details(
        List<IdentityV1Details.RegistryId> registryIds
    ) {
        return registryIds
            .stream()
            .map(r -> new BusinessPartnerIdentity.RegistryId(r.type(), r.value()))
            .toList();
    }

    private static List<BusinessPartnerIdentity.RegistryId> toRegistryIdsFromIdentityV2Details(
        List<IdentityV2Details.RegistryId> registryIds
    ) {
        return registryIds
            .stream()
            .map(r -> new BusinessPartnerIdentity.RegistryId(r.type(), r.value()))
            .toList();
    }
}
