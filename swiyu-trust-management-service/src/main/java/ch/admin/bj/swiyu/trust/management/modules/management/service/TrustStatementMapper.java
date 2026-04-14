package ch.admin.bj.swiyu.trust.management.modules.management.service;

import static java.util.Collections.emptyList;
import static java.util.Collections.emptyMap;

import ch.admin.bj.swiyu.trust.client.core.business.model.ProofOfPossessionDto;
import ch.admin.bj.swiyu.trust.client.core.business.model.TrustOnboardingSubmissionDto;
import ch.admin.bj.swiyu.trust.management.modules.management.api.*;
import ch.admin.bj.swiyu.trust.management.modules.management.domain.TrustStatementPartnerLink;
import ch.admin.bj.swiyu.trust.management.modules.management.domain.TrustStatementPartnerLinkStatus;
import ch.admin.bj.swiyu.trust.management.modules.management.domain.details.IdentityV1Details;
import ch.admin.bj.swiyu.trust.management.modules.management.domain.details.MetadataV1Details;
import ch.admin.bj.swiyu.trust.management.modules.management.domain.details.TrustStatementDetails;
import ch.admin.bj.swiyu.trust.management.modules.management.domain.details.TrustStatementType;
import ch.admin.bj.swiyu.trust.management.modules.management.domain.issuer.IssuerVcStatus;
import ch.admin.bj.swiyu.trust.management.modules.registry.api.DatastoreStatusDto;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.validation.constraints.NotNull;
import java.time.Instant;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import lombok.experimental.UtilityClass;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;

@UtilityClass
public class TrustStatementMapper {

    private final String createdBy = "createdBy"; // required by sonar as 3 times a string needs to be a constant
    private final List<String> allowedSortFields = List.of(
        "subject",
        "type",
        "status",
        "createdAt",
        createdBy,
        "updatedAt",
        "updatedBy"
    );

    public static TrustStatementPartnerLinkDto toTrustStatementPartnerLinkDto(
        TrustStatementPartnerLink source,
        IssuerVcStatus issuerVcStatus,
        DatastoreStatusDto trustRegistryStatus
    ) {
        var target = new TrustStatementPartnerLinkDto();
        target.setId(source.getId());
        target.setSubject(source.getSubject());
        target.setType(toTrustStatementTypeDto(source.getType()));
        target.setStatus(toTrustStatementPartnerLinkStatusDto(source.getStatus()));
        target.setValidFrom(source.getValidFrom());
        target.setValidUntil(source.getValidUntil());
        target.setLastModifiedAt(source.getAudit().getLastModifiedAt());
        target.setLastModifiedBy(source.getAudit().getLastModifiedBy());
        target.setCreatedAt(source.getAudit().getCreatedAt());
        target.setCreatedBy(source.getAudit().getCreatedBy());
        target.setVcStatus(toVcStatusDto(issuerVcStatus));
        target.setTrustRegistryStatus(toTrustRegistryStatusDto(trustRegistryStatus));
        target.setDetails(toAdditionalPropertiesMap(source.getDetails()));
        return target;
    }

    public TrustStatementPartnerLinkListItemDto toTrustStatementPartnerLinkListItemDto(
        TrustStatementPartnerLink source
    ) {
        return new TrustStatementPartnerLinkListItemDto(
            source.getId(),
            source.getSubject(),
            toTrustStatementTypeDto(source.getType()),
            toTrustStatementPartnerLinkStatusDto(source.getStatus()),
            source.getAudit().getLastModifiedAt(),
            source.getAudit().getLastModifiedBy(),
            source.getAudit().getCreatedAt(),
            source.getAudit().getCreatedBy()
        );
    }

    static IssuerVcStatusDto toVcStatusDto(IssuerVcStatus issuerVcStatus) {
        return switch (issuerVcStatus) {
            case UNKNOWN -> IssuerVcStatusDto.UNKNOWN;
            case INVALID -> IssuerVcStatusDto.INVALID;
            case VALID -> IssuerVcStatusDto.VALID;
        };
    }

    static TrustStatementPartnerLinkStatusDto toTrustStatementPartnerLinkStatusDto(
        TrustStatementPartnerLinkStatus status
    ) {
        return switch (status) {
            case ACTIVE -> TrustStatementPartnerLinkStatusDto.ACTIVE;
            case CONFIRMED -> TrustStatementPartnerLinkStatusDto.CONFIRMED;
            case INACTIVE -> TrustStatementPartnerLinkStatusDto.INACTIVE;
        };
    }

    public static Map<MetadataV1Details.Language, String> toMetadataV1LanguageMap(
        Map<TrustStatementPartnerLinkMetadataV1RequestDto.LanguageDto, String> source
    ) {
        if (source == null) {
            return emptyMap();
        }
        return source
            .entrySet()
            .stream()
            .map(entry -> Map.entry(toMetadataV1Language(entry.getKey()), entry.getValue()))
            .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));
    }

    public static MetadataV1Details.Language toMetadataV1Language(
        TrustStatementPartnerLinkMetadataV1RequestDto.LanguageDto source
    ) {
        return switch (source) {
            case EN -> MetadataV1Details.Language.EN;
            case DE_CH -> MetadataV1Details.Language.DE_CH;
            case FR_CH -> MetadataV1Details.Language.FR_CH;
            case IT_CH -> MetadataV1Details.Language.IT_CH;
            case RM_CH -> MetadataV1Details.Language.RM_CH;
        };
    }

    public static Map<IdentityV1Details.Language, String> toIdentityV1LanguageMap(
        Map<TrustStatementPartnerLinkIdentityV1RequestDto.LanguageDto, String> source
    ) {
        if (source == null) {
            return emptyMap();
        }
        return source
            .entrySet()
            .stream()
            .map(entry -> Map.entry(toIdentityV1Language(entry.getKey()), entry.getValue()))
            .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));
    }

    public static IdentityV1Details.Language toIdentityV1Language(
        TrustStatementPartnerLinkIdentityV1RequestDto.LanguageDto source
    ) {
        return switch (source) {
            case EN -> IdentityV1Details.Language.EN;
            case DE_CH -> IdentityV1Details.Language.DE_CH;
            case FR_CH -> IdentityV1Details.Language.FR_CH;
            case IT_CH -> IdentityV1Details.Language.IT_CH;
            case RM_CH -> IdentityV1Details.Language.RM_CH;
        };
    }

    public static TrustStatementTypeDto toTrustStatementTypeDto(TrustStatementType type) {
        return switch (type) {
            case TRUST_STATEMENT_METADATA_V1 -> TrustStatementTypeDto.METADATA_V1;
            case TRUST_STATEMENT_IDENTITY_V1 -> TrustStatementTypeDto.IDENTITY_V1;
            case TRUST_STATEMENT_ISSUANCE_V1 -> TrustStatementTypeDto.ISSUANCE_V1;
            case TRUST_STATEMENT_VERIFICATION_V1 -> TrustStatementTypeDto.VERIFICATION_V1;
        };
    }

    public static TrustStatementType toTrustStatementType(TrustStatementTypeDto type) {
        return switch (type) {
            case METADATA_V1 -> TrustStatementType.TRUST_STATEMENT_METADATA_V1;
            case IDENTITY_V1 -> TrustStatementType.TRUST_STATEMENT_IDENTITY_V1;
            case ISSUANCE_V1 -> TrustStatementType.TRUST_STATEMENT_ISSUANCE_V1;
            case VERIFICATION_V1 -> TrustStatementType.TRUST_STATEMENT_VERIFICATION_V1;
        };
    }

    public static TrustStatementPartnerLinkStatus toTrustStatementPartnerLinkStatus(
        TrustStatementPartnerLinkStatusDto source
    ) {
        return switch (source) {
            case ACTIVE -> TrustStatementPartnerLinkStatus.ACTIVE;
            case CONFIRMED -> TrustStatementPartnerLinkStatus.CONFIRMED;
            case INACTIVE -> TrustStatementPartnerLinkStatus.INACTIVE;
        };
    }

    public static Pageable mapPageableWithValidSortProperties(Pageable pageable) {
        // filter Pageable for invalid sort fields to not fail them silently
        pageable
            .getSort()
            .stream()
            .filter(c -> !allowedSortFields.contains(c.getProperty()))
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
                    case createdBy -> new Sort.Order(order.getDirection(), "audit.createdBy");
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

    public static TrustRegistryStatusDto toTrustRegistryStatusDto(DatastoreStatusDto source) {
        return switch (source) {
            case ACTIVE -> TrustRegistryStatusDto.ACTIVE;
            case DEACTIVATED -> TrustRegistryStatusDto.INACTIVE;
            case null -> TrustRegistryStatusDto.UNKNOWN;
            default -> TrustRegistryStatusDto.OTHER;
        };
    }

    public static List<IdentityV1Details.RegistryId> toIdentityV1RegistryIds(
        List<TrustStatementPartnerLinkIdentityV1RequestDto.RegistryIdDto> source
    ) {
        if (source == null) {
            return emptyList();
        }
        return source
            .stream()
            .map(registryId -> new IdentityV1Details.RegistryId(registryId.type(), registryId.value()))
            .toList();
    }

    public static List<
        TrustStatementPartnerLinkIdentityV1RequestDto
    > toTrustStatementPartnerLinkIdentityV1RequestDtoList(
        TrustOnboardingSubmissionDto submission,
        Instant validFrom,
        Instant validUntil
    ) {
        return submission
            .getProofOfPossessions()
            .stream()
            .map(ProofOfPossessionDto::getDid)
            .map(did ->
                new TrustStatementPartnerLinkIdentityV1RequestDto(
                    did,
                    validFrom,
                    validUntil,
                    toPartnerName(submission),
                    submission.getIsGovActor(),
                    toIdentityV1RegistryIds(submission.getRegistryIds())
                )
            )
            .toList();
    }

    private static Map<TrustStatementPartnerLinkIdentityV1RequestDto.LanguageDto, String> toPartnerName(
        TrustOnboardingSubmissionDto submission
    ) {
        Map<TrustStatementPartnerLinkIdentityV1RequestDto.LanguageDto, String> partnerName = new HashMap<>();
        if (submission.getEntityName().getEn() != null) {
            partnerName.put(
                TrustStatementPartnerLinkIdentityV1RequestDto.LanguageDto.EN,
                submission.getEntityName().getEn()
            );
        }
        if (submission.getEntityName().getDe() != null) {
            partnerName.put(
                TrustStatementPartnerLinkIdentityV1RequestDto.LanguageDto.DE_CH,
                submission.getEntityName().getDe()
            );
        }
        if (submission.getEntityName().getFr() != null) {
            partnerName.put(
                TrustStatementPartnerLinkIdentityV1RequestDto.LanguageDto.FR_CH,
                submission.getEntityName().getFr()
            );
        }
        if (submission.getEntityName().getIt() != null) {
            partnerName.put(
                TrustStatementPartnerLinkIdentityV1RequestDto.LanguageDto.IT_CH,
                submission.getEntityName().getIt()
            );
        }
        if (submission.getEntityName().getRm() != null) {
            partnerName.put(
                TrustStatementPartnerLinkIdentityV1RequestDto.LanguageDto.RM_CH,
                submission.getEntityName().getRm()
            );
        }
        return partnerName;
    }

    private static List<TrustStatementPartnerLinkIdentityV1RequestDto.RegistryIdDto> toIdentityV1RegistryIds(
        Map<String, String> source
    ) {
        var list = new ArrayList<TrustStatementPartnerLinkIdentityV1RequestDto.RegistryIdDto>();
        source.forEach((key, value) ->
            list.add(new TrustStatementPartnerLinkIdentityV1RequestDto.RegistryIdDto(key, value))
        );
        return list;
    }

    private static Map<String, Object> toAdditionalPropertiesMap(@NotNull TrustStatementDetails data) {
        var objectMapper = new ObjectMapper();
        return objectMapper.convertValue(data, new TypeReference<>() {});
    }
}
