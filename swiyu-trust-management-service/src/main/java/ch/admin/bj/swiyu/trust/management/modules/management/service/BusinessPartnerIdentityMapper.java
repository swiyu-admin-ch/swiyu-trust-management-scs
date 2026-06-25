package ch.admin.bj.swiyu.trust.management.modules.management.service;

import ch.admin.bj.swiyu.trust.client.core.business.internal.model.BusinessPartnerTypeDto;
import ch.admin.bj.swiyu.trust.client.core.business.internal.model.MultiLanguageTextDto;
import ch.admin.bj.swiyu.trust.client.core.business.internal.model.TrustOnboardingSubmissionDto;
import ch.admin.bj.swiyu.trust.management.modules.management.api.IdentityV1RequestDto;
import ch.admin.bj.swiyu.trust.management.modules.management.api.IdentityV2RequestDto;
import ch.admin.bj.swiyu.trust.management.modules.management.domain.BusinessPartnerIdentity;
import ch.admin.bj.swiyu.trust.management.modules.management.domain.TrustStatementPartnerLink;
import ch.admin.bj.swiyu.trust.management.modules.management.domain.details.IdentityV1Details;
import ch.admin.bj.swiyu.trust.management.modules.management.domain.details.IdentityV2Details;
import java.time.Duration;
import java.time.Instant;
import java.util.EnumMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import lombok.experimental.UtilityClass;
import org.jetbrains.annotations.NotNull;

@UtilityClass
public class BusinessPartnerIdentityMapper {

    public static BusinessPartnerIdentity toBusinessPartnerIdentity(
        TrustOnboardingSubmissionDto trustOnboardingSubmission
    ) {
        return new BusinessPartnerIdentity(
            trustOnboardingSubmission.getPartnerId(),
            toEntityNameFromMultiLanguage(trustOnboardingSubmission.getEntityName()),
            getDefaultLanguageFromMultiLanguageTextDto(trustOnboardingSubmission.getEntityName()),
            trustOnboardingSubmission.getBusinessPartnerType() == BusinessPartnerTypeDto.GOVERNMENTAL_INSTITUTION,
            trustOnboardingSubmission
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
                    toEntityNameFromIdentityV1Details(details.getEntityName()),
                    getDefaultLanguageFromIdentityV1DetailsLanguageMap(details.getEntityName()),
                    details.getIsStateActor(),
                    toRegistryIdsFromIdentityV1Details(details.getRegistryIds())
                );
            }
            case TRUST_STATEMENT_IDENTITY_V2 -> {
                var details = (IdentityV2Details) partnerLink.getDetails();
                yield new BusinessPartnerIdentity(
                    partnerLink.getPartnerId(),
                    toEntityNameFromIdentityV2Details(details.getEntityName()),
                    getDefaultLanguageFromIdentityV2DetailsLanguageMap(details.getEntityName()),
                    details.getIsStateActor(),
                    toRegistryIdsFromIdentityV2Details(details.getRegistryIds())
                );
            }
            default -> throw new IllegalArgumentException(
                "Cannot get business partner identity from partner link type: " + partnerLink.getType()
            );
        };
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
            BusinessPartnerIdentityMapper.toRequestLocalizedText(
                businessPartnerIdentity.entityName(),
                businessPartnerIdentity.defaultLanguage()
            ),
            businessPartnerIdentity.isStateActor(),
            BusinessPartnerIdentityMapper.toRequestRegistryIdsV2(businessPartnerIdentity.registryIds())
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
            BusinessPartnerIdentityMapper.toRequestLocalizedText(
                businessPartnerIdentity.entityName(),
                businessPartnerIdentity.defaultLanguage()
            ),
            businessPartnerIdentity.isStateActor(),
            BusinessPartnerIdentityMapper.toRequestRegistryIdsV1(businessPartnerIdentity.registryIds())
        );
    }

    public static Map<String, String> toRequestLocalizedText(
        Map<BusinessPartnerIdentity.Language, String> source,
        BusinessPartnerIdentity.Language defaultLanguage
    ) {
        if (source == null || source.isEmpty()) {
            return Map.of();
        }
        var result = new LinkedHashMap<String, String>();
        var defaultValue = source.getOrDefault(defaultLanguage, source.values().iterator().next());
        result.put("default", defaultValue);
        source.forEach((lang, text) -> result.put(lang.getLanguageCode(), text));
        return result;
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

    private static BusinessPartnerIdentity.Language getDefaultLanguageFromMultiLanguageTextDto(
        MultiLanguageTextDto entityName
    ) {
        if (entityName.getDe() != null && !entityName.getDe().isEmpty()) {
            return BusinessPartnerIdentity.Language.DE_CH;
        } else if (entityName.getEn() != null && !entityName.getEn().isEmpty()) {
            return BusinessPartnerIdentity.Language.EN;
        } else if (entityName.getFr() != null && !entityName.getFr().isEmpty()) {
            return BusinessPartnerIdentity.Language.FR_CH;
        } else if (entityName.getIt() != null && !entityName.getIt().isEmpty()) {
            return BusinessPartnerIdentity.Language.IT_CH;
        } else if (entityName.getRm() != null && !entityName.getRm().isEmpty()) {
            return BusinessPartnerIdentity.Language.RM_CH;
        } else {
            throw new IllegalStateException("Could not detect default language");
        }
    }

    private static Map<BusinessPartnerIdentity.Language, String> toEntityNameFromMultiLanguage(
        MultiLanguageTextDto entityName
    ) {
        EnumMap<BusinessPartnerIdentity.Language, String> ret = new EnumMap<>(BusinessPartnerIdentity.Language.class);

        if (entityName.getDe() != null && !entityName.getDe().isEmpty()) {
            ret.put(BusinessPartnerIdentity.Language.DE_CH, entityName.getDe());
        }
        if (entityName.getEn() != null && !entityName.getEn().isEmpty()) {
            ret.put(BusinessPartnerIdentity.Language.EN, entityName.getEn());
        }
        if (entityName.getFr() != null && !entityName.getFr().isEmpty()) {
            ret.put(BusinessPartnerIdentity.Language.FR_CH, entityName.getFr());
        }
        if (entityName.getIt() != null && !entityName.getIt().isEmpty()) {
            ret.put(BusinessPartnerIdentity.Language.IT_CH, entityName.getIt());
        }
        if (entityName.getRm() != null && !entityName.getRm().isEmpty()) {
            ret.put(BusinessPartnerIdentity.Language.RM_CH, entityName.getRm());
        }

        return ret;
    }

    private static BusinessPartnerIdentity.Language getDefaultLanguageFromIdentityV1DetailsLanguageMap(
        Map<IdentityV1Details.Language, String> entityName
    ) {
        if (entityName.containsKey(IdentityV1Details.Language.DE_CH)) {
            return BusinessPartnerIdentity.Language.DE_CH;
        } else if (entityName.containsKey(IdentityV1Details.Language.EN)) {
            return BusinessPartnerIdentity.Language.EN;
        } else {
            return BusinessPartnerIdentity.Language.fromLanguageCode(
                entityName
                    .keySet()
                    .stream()
                    .findFirst()
                    .orElseThrow(() -> new IllegalArgumentException("EntityName needs at least one entry."))
                    .getJsonValue()
            );
        }
    }

    private static BusinessPartnerIdentity.Language getDefaultLanguageFromIdentityV2DetailsLanguageMap(
        Map<IdentityV2Details.Language, String> entityName
    ) {
        if (entityName.containsKey(IdentityV2Details.Language.DE_CH)) {
            return BusinessPartnerIdentity.Language.DE_CH;
        } else if (entityName.containsKey(IdentityV2Details.Language.EN)) {
            return BusinessPartnerIdentity.Language.EN;
        } else {
            return BusinessPartnerIdentity.Language.fromLanguageCode(
                entityName
                    .keySet()
                    .stream()
                    .findFirst()
                    .orElseThrow(() -> new IllegalArgumentException("EntityName needs at least one entry."))
                    .getLanguageCode()
            );
        }
    }

    private static Map<BusinessPartnerIdentity.Language, String> toEntityNameFromIdentityV1Details(
        Map<IdentityV1Details.Language, String> entityName
    ) {
        return entityName
            .entrySet()
            .stream()
            .collect(
                Collectors.toMap(
                    e -> BusinessPartnerIdentity.Language.valueOf(e.getKey().toString()),
                    Map.Entry::getValue
                )
            );
    }

    private static Map<BusinessPartnerIdentity.Language, String> toEntityNameFromIdentityV2Details(
        Map<IdentityV2Details.Language, String> entityName
    ) {
        return entityName
            .entrySet()
            .stream()
            .collect(
                Collectors.toMap(
                    e -> BusinessPartnerIdentity.Language.valueOf(e.getKey().toString()),
                    Map.Entry::getValue
                )
            );
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
