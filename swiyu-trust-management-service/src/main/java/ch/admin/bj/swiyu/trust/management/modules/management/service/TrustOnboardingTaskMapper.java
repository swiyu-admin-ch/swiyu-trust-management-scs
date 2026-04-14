package ch.admin.bj.swiyu.trust.management.modules.management.service;

import static ch.admin.bj.swiyu.trust.management.modules.ui.api.BusinessPartnerTypeDto.GOVERNMENTAL_INSTITUTION;
import static ch.admin.bj.swiyu.trust.management.modules.ui.api.BusinessPartnerTypeDto.UNKNOWN;
import static java.util.Collections.emptyList;
import static org.springframework.util.CollectionUtils.isEmpty;
import static org.springframework.util.StringUtils.hasText;

import ch.admin.bj.swiyu.trust.client.core.business.model.AddressDto;
import ch.admin.bj.swiyu.trust.client.core.business.model.ContactDto;
import ch.admin.bj.swiyu.trust.client.core.business.model.LanguageDto;
import ch.admin.bj.swiyu.trust.client.core.business.model.ProofOfPossessionDto;
import ch.admin.bj.swiyu.trust.client.core.business.model.TrustOnboardingSubmissionDto;
import ch.admin.bj.swiyu.trust.management.modules.management.api.TrustOnboardingTaskActionDto;
import ch.admin.bj.swiyu.trust.management.modules.management.domain.*;
import ch.admin.bj.swiyu.trust.management.modules.ui.api.BusinessPartnerTypeDto;
import ch.admin.bj.swiyu.trust.management.modules.ui.api.TrustOnboardingTaskContactTypeDto;
import ch.admin.bj.swiyu.trust.management.modules.ui.api.TrustOnboardingTaskDto;
import ch.admin.bj.swiyu.trust.management.modules.ui.api.TrustOnboardingTaskListItemDto;
import ch.admin.bj.swiyu.trust.management.modules.ui.api.TrustOnboardingTaskStatusDto;
import jakarta.validation.constraints.NotNull;
import java.time.*;
import java.util.*;
import lombok.experimental.*;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@UtilityClass
public class TrustOnboardingTaskMapper {

    public static TrustOnboardingTaskListItemDto toTrustOnboardingTaskListItemDto(
        TrustOnboardingTask task,
        Set<TrustOnboardingTaskActionDto> allowedActions
    ) {
        return toTaskListItemDto(task, allowedActions);
    }

    public static TrustOnboardingTaskListItemDto toTaskListItemDto(
        TrustTask task,
        Set<TrustOnboardingTaskActionDto> allowedActions
    ) {
        return new TrustOnboardingTaskListItemDto(
            task.getId(),
            toMultiLanguageTextDto(task.getPartnerName()),
            task.getSubmittedAt(),
            task.getDueAt(),
            toTrustOnboardingTaskStatusDto(task.getStatus()),
            task.getAssignee(),
            task.getTaskType().name(),
            allowedActions
        );
    }

    static TrustOnboardingTaskStatusDto toTrustOnboardingTaskStatusDto(TrustTaskStatus source) {
        return switch (source) {
            case REJECTED -> TrustOnboardingTaskStatusDto.REJECTED;
            case ACCEPTED -> TrustOnboardingTaskStatusDto.ACCEPTED;
            case OPENED -> TrustOnboardingTaskStatusDto.OPENED;
            case INFORMATION_REQUESTED -> TrustOnboardingTaskStatusDto.INFORMATION_REQUESTED;
        };
    }

    public static ch.admin.bj.swiyu.trust.management.modules.ui.api.MultiLanguageTextDto toMultiLanguageTextDto(
        PartnerName partnerName
    ) {
        return new ch.admin.bj.swiyu.trust.management.modules.ui.api.MultiLanguageTextDto(
            partnerName.getPartnerNameDe(),
            partnerName.getPartnerNameFr(),
            partnerName.getPartnerNameIt(),
            partnerName.getPartnerNameEn(),
            partnerName.getPartnerNameRm()
        );
    }

    public static TrustOnboardingTaskDto toTrustOnboardingTaskDto(
        Set<TrustOnboardingTaskActionDto> allowedActions,
        TrustOnboardingTask task,
        TrustOnboardingSubmissionDto submission
    ) {
        var entityName = toNameLanguageDtoMap(submission.getEntityName());
        var correspondanceLanguage = toCorrespondanceLanguageDto(submission.getCorrespondingLanguage());
        return new TrustOnboardingTaskDto(
            task.getId(),
            task.getAssignee(),
            task.getSubmittedAt(),
            task.getDueAt(),
            toTrustOnboardingTaskStatusDto(task.getStatus()),
            toBusinessPartnerTypeDto(submission),
            toUid(submission.getRegistryIds()),
            submission.getIsRegisteredInCommercialRegister(),
            toEntityNameDefault(entityName, correspondanceLanguage),
            entityName,
            toAddressStreet(submission.getAddress()),
            toAddressZipCodeCity(submission.getAddress()),
            toAddressCountry(submission.getAddress()),
            submission.getEntityEmail(),
            correspondanceLanguage,
            toContactDto(submission.getContactPerson()),
            emptyList(), // TODO EID-5459
            toDidDto(submission.getProofOfPossessions()),
            allowedActions
        );
    }

    private static String toEntityNameDefault(
        Map<TrustOnboardingTaskDto.LanguageDto, String> entityName,
        @NotNull TrustOnboardingTaskDto.LanguageDto defaultLang
    ) {
        if (isEmpty(entityName)) {
            return null;
        }
        var name = entityName.get(defaultLang);
        if (!hasText(name)) {
            log.error("missing entity name for default language {}", defaultLang);
            return null;
        }
        return name;
    }

    private static List<TrustOnboardingTaskDto.DidDto> toDidDto(List<ProofOfPossessionDto> source) {
        if (isEmpty(source)) {
            return emptyList();
        }
        return source
            .stream()
            .map(pop -> new TrustOnboardingTaskDto.DidDto(pop.getDid(), pop.getVerifiedAt()))
            .toList();
    }

    private static String toAddressCountry(AddressDto address) {
        return address != null ? address.getCountry() : null;
    }

    private static String toAddressZipCodeCity(AddressDto address) {
        if (address == null) {
            return null;
        }
        return address.getPostalCode() + ", " + address.getCity();
    }

    private static String toAddressStreet(AddressDto address) {
        return address != null ? address.getStreet() : null;
    }

    private static String toUid(Map<String, String> registryIds) {
        if (registryIds == null) {
            return null;
        }
        return registryIds
            .entrySet()
            .stream()
            .filter(e -> "uid".equalsIgnoreCase(e.getKey()))
            .map(Map.Entry::getValue)
            .filter(Objects::nonNull)
            .findFirst()
            .orElse(null);
    }

    private static BusinessPartnerTypeDto toBusinessPartnerTypeDto(TrustOnboardingSubmissionDto submission) {
        // Temporary duplicate mapping until EID-5292 is done
        var partnerType = submission.getBusinessPartnerType();
        if (partnerType != null) {
            return switch (partnerType) {
                case BUSINESS -> BusinessPartnerTypeDto.BUSINESS;
                case INDIVIDUAL -> BusinessPartnerTypeDto.INDIVIDUAL;
                case GOVERNMENTAL_INSTITUTION -> BusinessPartnerTypeDto.GOVERNMENTAL_INSTITUTION;
                case UNKNOWN -> BusinessPartnerTypeDto.UNKNOWN;
            };
        }

        // Contracting will happen with EID-5292
        var isGovActor = submission.getIsGovActor(); // NOSONAR
        var type = Boolean.TRUE.equals(isGovActor) ? GOVERNMENTAL_INSTITUTION : UNKNOWN;
        if (UNKNOWN.equals(type)) {
            log.warn(
                "Received trust onboarding submission for non governmental actor. " +
                    "The partner type will be shown as UNKOWN in the UI. If the real partner type shall be shown" +
                    "the submission API of core business service should be updated to provide this infomation. "
            );
        }
        return type;
    }

    private static Map<TrustOnboardingTaskDto.LanguageDto, String> toNameLanguageDtoMap(
        ch.admin.bj.swiyu.trust.client.core.business.model.MultiLanguageTextDto source
    ) {
        if (source == null) {
            return Collections.emptyMap();
        }
        Map<TrustOnboardingTaskDto.LanguageDto, String> result = new EnumMap<>(
            TrustOnboardingTaskDto.LanguageDto.class
        );
        if (source.getDe() != null && !source.getDe().isBlank()) {
            result.put(TrustOnboardingTaskDto.LanguageDto.DE_CH, source.getDe());
        }
        if (source.getEn() != null && !source.getEn().isBlank()) {
            result.put(TrustOnboardingTaskDto.LanguageDto.EN, source.getEn());
        }
        if (source.getFr() != null && !source.getFr().isBlank()) {
            result.put(TrustOnboardingTaskDto.LanguageDto.FR_CH, source.getFr());
        }
        if (source.getIt() != null && !source.getIt().isBlank()) {
            result.put(TrustOnboardingTaskDto.LanguageDto.IT_CH, source.getIt());
        }
        if (source.getRm() != null && !source.getRm().isBlank()) {
            result.put(TrustOnboardingTaskDto.LanguageDto.RM_CH, source.getRm());
        }
        return result;
    }

    private static List<TrustOnboardingTaskDto.ContactDto> toContactDto(ContactDto source) {
        if (source == null) {
            return emptyList();
        }
        return List.of(
            new TrustOnboardingTaskDto.ContactDto(
                "%s %s".formatted(source.getFirstName(), source.getLastName()),
                TrustOnboardingTaskContactTypeDto.CONTACT_PERSON,
                source.getPhone(),
                source.getEmail()
            )
        );
    }

    private static TrustOnboardingTaskDto.LanguageDto toCorrespondanceLanguageDto(LanguageDto source) {
        if (source == null) {
            return null;
        }
        return switch (source) {
            case DE -> TrustOnboardingTaskDto.LanguageDto.DE_CH;
            case EN -> TrustOnboardingTaskDto.LanguageDto.EN;
            case FR -> TrustOnboardingTaskDto.LanguageDto.FR_CH;
            case IT -> TrustOnboardingTaskDto.LanguageDto.IT_CH;
            case RM -> TrustOnboardingTaskDto.LanguageDto.RM_CH;
        };
    }
}
