package ch.admin.bj.swiyu.trust.management.modules.management.service;

import static java.util.Collections.emptyList;
import static java.util.Collections.emptyMap;

import ch.admin.bj.swiyu.trust.client.core.business.internal.model.ProofOfPossessionDto;
import ch.admin.bj.swiyu.trust.client.core.business.internal.model.TrustOnboardingSubmissionDto;
import ch.admin.bj.swiyu.trust.management.modules.common.i18n.LocalizedMapConstants;
import ch.admin.bj.swiyu.trust.management.modules.management.api.*;
import ch.admin.bj.swiyu.trust.management.modules.management.domain.BusinessPartnerIdentity;
import ch.admin.bj.swiyu.trust.management.modules.management.domain.TrustStatementPartnerLink;
import ch.admin.bj.swiyu.trust.management.modules.management.domain.TrustStatementPartnerLinkStatus;
import ch.admin.bj.swiyu.trust.management.modules.management.domain.details.*;
import ch.admin.bj.swiyu.trust.management.modules.management.domain.issuer.IssuerVcStatus;
import ch.admin.bj.swiyu.trust.management.modules.registry.api.DatastoreStatusDto;
import ch.admin.bj.swiyu.trust.management.modules.registry.api.StatementDto;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.validation.constraints.NotNull;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
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
        ObjectMapper objectMapper,
        TrustStatementPartnerLink source,
        IssuerVcStatus issuerVcStatus,
        TrustRegistryStatusDto trustRegistryStatus
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
        target.setTrustRegistryStatus(trustRegistryStatus);
        target.setDetails(toAdditionalPropertiesMap(objectMapper, source.getDetails()));
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

    public static Map<IdentityV1Details.Language, String> toIdentityV1LanguageMap(Map<String, String> source) {
        if (source == null) {
            return emptyMap();
        }
        var result = new EnumMap<IdentityV1Details.Language, String>(IdentityV1Details.Language.class);
        source.forEach((locale, text) -> {
            if (LocalizedMapConstants.DEFAULT_VALUE_KEY.equals(locale)) {
                result.put(IdentityV1Details.Language.DEFAULT, text);
            } else {
                result.put(IdentityV1Details.Language.fromJsonValue(locale), text);
            }
        });
        return result;
    }

    public static Map<IdentityV2Details.Language, String> toIdentityV2LanguageMap(Map<String, String> source) {
        if (source == null) {
            return emptyMap();
        }
        var result = new EnumMap<IdentityV2Details.Language, String>(IdentityV2Details.Language.class);
        source.forEach((locale, text) -> {
            if (LocalizedMapConstants.DEFAULT_VALUE_KEY.equals(locale)) {
                result.put(IdentityV2Details.Language.DEFAULT, text);
            } else {
                result.put(toIdentityV2Language(locale), text);
            }
        });
        return result;
    }

    public static Map<VerificationQueryV2Details.Language, String> toVerificationQueryV2LanguageMap(
        Map<String, String> source
    ) {
        if (source == null) {
            return emptyMap();
        }
        var result = new EnumMap<VerificationQueryV2Details.Language, String>(
            VerificationQueryV2Details.Language.class
        );
        source.forEach((locale, text) -> {
            if (LocalizedMapConstants.DEFAULT_VALUE_KEY.equals(locale)) {
                result.put(VerificationQueryV2Details.Language.DEFAULT, text);
            } else {
                result.put(toVerificationQueryV2Language(locale), text);
            }
        });
        return result;
    }

    public static VerificationQueryV2Details.VerificationRequestObject toVerificationQueryV2VerificationRequestObject(
        VerificationQueryV2RequestDto.VerificationRequestObjectDto source
    ) {
        return new VerificationQueryV2Details.VerificationRequestObject(source.type(), source.scope(), source.query());
    }

    public static Map<
        ProtectedIssuanceAuthorizationV2Details.Language,
        String
    > toProtectedIssuanceAuthorizationV2DetailsLanguageMap(Map<String, String> source) {
        if (source == null) {
            return emptyMap();
        }
        var result = new EnumMap<ProtectedIssuanceAuthorizationV2Details.Language, String>(
            ProtectedIssuanceAuthorizationV2Details.Language.class
        );
        source.forEach((locale, text) -> {
            if (LocalizedMapConstants.DEFAULT_VALUE_KEY.equals(locale)) {
                result.put(ProtectedIssuanceAuthorizationV2Details.Language.DEFAULT, text);
            } else {
                result.put(toProtectedIssuanceAuthorizationV2Language(locale), text);
            }
        });
        return result;
    }

    public static TrustStatementTypeDto toTrustStatementTypeDto(TrustStatementPartnerLinkType type) {
        return switch (type) {
            case TRUST_STATEMENT_IDENTITY_V1 -> TrustStatementTypeDto.IDENTITY_V1;
            case TRUST_STATEMENT_ISSUANCE_V1 -> TrustStatementTypeDto.ISSUANCE_V1;
            case TRUST_STATEMENT_VERIFICATION_V1 -> TrustStatementTypeDto.VERIFICATION_V1;
            case TRUST_STATEMENT_PROTECTED_VERIFICATION_AUTHORIZATION_V2 -> TrustStatementTypeDto.PROTECTED_VERIFICATION_AUTHORIZATION_V2;
            case TRUST_STATEMENT_PROTECTED_ISSUANCE_AUTHORIZATION_V2 -> TrustStatementTypeDto.PROTECTED_ISSUANCE_AUTHORIZATION_V2;
            case TRUST_LIST_STATEMENT_NON_COMPLIANCE_V2 -> TrustStatementTypeDto.NON_COMPLIANCE_V2;
            case TRUST_LIST_STATEMENT_PROTECTED_ISSUANCE_V2 -> TrustStatementTypeDto.PROTECTED_ISSUANCE_V2;
            case PUBLIC_STATEMENT_VERIFICATION_QUERY_V2 -> TrustStatementTypeDto.PUBLIC_STATEMENT_VERIFICATION_QUERY_V2;
            case TRUST_STATEMENT_IDENTITY_V2 -> TrustStatementTypeDto.IDENTITY_V2;
        };
    }

    public static TrustStatementPartnerLinkType toTrustStatementType(TrustStatementTypeDto type) {
        return switch (type) {
            case IDENTITY_V1 -> TrustStatementPartnerLinkType.TRUST_STATEMENT_IDENTITY_V1;
            case ISSUANCE_V1 -> TrustStatementPartnerLinkType.TRUST_STATEMENT_ISSUANCE_V1;
            case VERIFICATION_V1 -> TrustStatementPartnerLinkType.TRUST_STATEMENT_VERIFICATION_V1;
            case PROTECTED_VERIFICATION_AUTHORIZATION_V2 -> TrustStatementPartnerLinkType.TRUST_STATEMENT_PROTECTED_VERIFICATION_AUTHORIZATION_V2;
            case PROTECTED_ISSUANCE_AUTHORIZATION_V2 -> TrustStatementPartnerLinkType.TRUST_STATEMENT_PROTECTED_ISSUANCE_AUTHORIZATION_V2;
            case NON_COMPLIANCE_V2 -> TrustStatementPartnerLinkType.TRUST_LIST_STATEMENT_NON_COMPLIANCE_V2;
            case PROTECTED_ISSUANCE_V2 -> TrustStatementPartnerLinkType.TRUST_LIST_STATEMENT_PROTECTED_ISSUANCE_V2;
            case PUBLIC_STATEMENT_VERIFICATION_QUERY_V2 -> TrustStatementPartnerLinkType.PUBLIC_STATEMENT_VERIFICATION_QUERY_V2;
            case IDENTITY_V2 -> TrustStatementPartnerLinkType.TRUST_STATEMENT_IDENTITY_V2;
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

    public static TrustRegistryStatusDto toTrustRegistryStatusDto(StatementDto source) {
        var ret = TrustRegistryStatusDto.ACTIVE;
        if (!source.isActiveInStatusList() || source.isSoftDeleted()) {
            ret = TrustRegistryStatusDto.INACTIVE;
        }
        return ret;
    }

    public static List<IdentityV1Details.RegistryId> toIdentityV1RegistryIds(
        List<IdentityV1RequestDto.RegistryIdDto> source
    ) {
        if (source == null) {
            return emptyList();
        }
        return source
            .stream()
            .map(registryId -> new IdentityV1Details.RegistryId(registryId.type(), registryId.value()))
            .toList();
    }

    public static List<IdentityV2Details.RegistryId> toIdentityV2RegistryIds(
        List<IdentityV2RequestDto.RegistryIdDto> source
    ) {
        if (source == null) {
            return emptyList();
        }
        return source
            .stream()
            .map(registryId -> new IdentityV2Details.RegistryId(registryId.type(), registryId.value()))
            .toList();
    }

    public static List<
        ProtectedVerificationAuthorizationV2Details.AuthorizableField
    > toProtectedVerificationAuthorizationV2AuthorizableField(
        List<ProtectedVerificationAuthorizationV2RequestDto.AuthorizableFieldDto> source
    ) {
        return source
            .stream()
            .map(TrustStatementMapper::toProtectedVerificationAuthorizationV2AuthorizableField)
            .toList();
    }

    public static ProtectedVerificationAuthorizationV2Details.AuthorizableField toProtectedVerificationAuthorizationV2AuthorizableField(
        ProtectedVerificationAuthorizationV2RequestDto.AuthorizableFieldDto source
    ) {
        return switch (source) {
            case AHV_NUMBER -> ProtectedVerificationAuthorizationV2Details.AuthorizableField.AHV_NUMBER;
        };
    }

    public static List<IdentityV1RequestDto> toTrustStatementPartnerLinkIdentityV1RequestDtoList(
        TrustOnboardingSubmissionDto submission,
        BusinessPartnerIdentity businessPartnerIdentity
    ) {
        return submission
            .getProofOfPossessions()
            .stream()
            .map(ProofOfPossessionDto::getDid)
            .map(did ->
                BusinessPartnerIdentityMapper.toTrustStatementPartnerLinkIdentityV1RequestDto(
                    did,
                    businessPartnerIdentity
                )
            )
            .toList();
    }

    public static List<IdentityV2RequestDto> toTrustStatementPartnerLinkIdentityV2RequestDtoList(
        TrustOnboardingSubmissionDto submission,
        BusinessPartnerIdentity businessPartnerIdentity
    ) {
        return submission
            .getProofOfPossessions()
            .stream()
            .map(ProofOfPossessionDto::getDid)
            .map(did ->
                BusinessPartnerIdentityMapper.toTrustStatementPartnerLinkIdentityV2RequestDto(
                    did,
                    businessPartnerIdentity
                )
            )
            .toList();
    }

    public static ProtectedIssuanceAuthorizationV2Details.ProtectedIssuanceAuthorization toProtectedIssuanceAuthorizationDto(
        ProtectedIssuanceAuthorizationV2RequestDto.ProtectedIssuanceAuthorizationDto source
    ) {
        return new ProtectedIssuanceAuthorizationV2Details.ProtectedIssuanceAuthorization(
            source.vct(),
            toProtectedIssuanceAuthorizationV2DetailsLanguageMap(source.vctName()),
            toProtectedIssuanceAuthorizationV2DetailsLanguageMap(source.reason())
        );
    }

    private static IdentityV2Details.Language toIdentityV2Language(String locale) {
        for (var lang : IdentityV2Details.Language.values()) {
            if (lang.getLanguageCode().equals(locale)) {
                return lang;
            }
        }
        throw new IllegalArgumentException("Unsupported locale: " + locale);
    }

    private static VerificationQueryV2Details.Language toVerificationQueryV2Language(String locale) {
        for (var lang : VerificationQueryV2Details.Language.values()) {
            if (lang.getLanguageCode().equals(locale)) {
                return lang;
            }
        }
        throw new IllegalArgumentException("Unsupported locale: " + locale);
    }

    private static ProtectedIssuanceAuthorizationV2Details.Language toProtectedIssuanceAuthorizationV2Language(
        String locale
    ) {
        for (var lang : ProtectedIssuanceAuthorizationV2Details.Language.values()) {
            if (lang.getLanguageCode().equals(locale)) {
                return lang;
            }
        }
        throw new IllegalArgumentException("Unsupported locale: " + locale);
    }

    private static Map<String, Object> toAdditionalPropertiesMap(
        ObjectMapper objectMapper,
        @NotNull TrustStatementDetails data
    ) {
        return objectMapper.convertValue(data, new TypeReference<>() {});
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
}
