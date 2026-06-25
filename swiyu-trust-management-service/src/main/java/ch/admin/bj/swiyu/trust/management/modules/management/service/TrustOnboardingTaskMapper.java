package ch.admin.bj.swiyu.trust.management.modules.management.service;

import static java.util.Collections.emptyList;
import static org.springframework.util.CollectionUtils.isEmpty;
import static org.springframework.util.StringUtils.hasText;

import ch.admin.bj.swiyu.trust.client.core.business.internal.model.*;
import ch.admin.bj.swiyu.trust.management.modules.management.api.TrustOnboardingTaskActionDto;
import ch.admin.bj.swiyu.trust.management.modules.management.domain.PartnerName;
import ch.admin.bj.swiyu.trust.management.modules.management.domain.TrustOnboardingTask;
import ch.admin.bj.swiyu.trust.management.modules.management.domain.TrustTask;
import ch.admin.bj.swiyu.trust.management.modules.management.domain.TrustTaskStatus;
import ch.admin.bj.swiyu.trust.management.modules.ui.api.BusinessPartnerTypeDto;
import ch.admin.bj.swiyu.trust.management.modules.ui.api.TrustOnboardingTaskContactTypeDto;
import ch.admin.bj.swiyu.trust.management.modules.ui.api.TrustOnboardingTaskDto;
import ch.admin.bj.swiyu.trust.management.modules.ui.api.TrustOnboardingTaskListItemDto;
import ch.admin.bj.swiyu.trust.management.modules.ui.api.TrustOnboardingTaskStatusDto;
import jakarta.validation.constraints.NotNull;
import java.util.*;
import lombok.experimental.UtilityClass;
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
        var correspondenceLanguage = toCorrespondanceLanguageDto(submission.getCorrespondingLanguage());
        return new TrustOnboardingTaskDto(
            task.getId(),
            task.getAssignee(),
            task.getSubmittedAt(),
            task.getDueAt(),
            toTrustOnboardingTaskStatusDto(task.getStatus()),
            toBusinessPartnerTypeDto(submission),
            toUid(submission.getRegistryIds()),
            submission.getIsRegisteredInCommercialRegister(),
            toEntityNameDefault(entityName, correspondenceLanguage),
            entityName,
            toAddressStreet(submission.getAddress()),
            toAddressZipCodeCity(submission.getAddress()),
            toAddressCountry(submission.getAddress()),
            submission.getEntityEmail(),
            correspondenceLanguage,
            toContactDto(submission),
            toDidDto(submission.getProofOfPossessions()),
            allowedActions
        );
    }

    private static String toEntityNameDefault(
        Map<TrustOnboardingTaskDto.LanguageDto, String> entityNames,
        @NotNull TrustOnboardingTaskDto.LanguageDto correspondenceLanguage
    ) {
        if (isEmpty(entityNames)) {
            return null;
        }
        // partners currently only submit the name in one language
        // in that case, we take that name as default
        var names = entityNames
            .entrySet()
            .stream()
            .filter(e -> hasText(e.getValue()))
            .toList();
        if (names.size() == 1) {
            return names.getFirst().getValue();
        }
        // at this point there are multiple languages. we are guessing the default name by using the correspondence
        // by using the correspondence language.This is a tech dept, which was introduced by implementing the MVP
        // version of the trust onboarding.
        var name = entityNames.get(correspondenceLanguage);
        if (hasText(name)) {
            return name; // taking name in correspondence language as default
        } else {
            var fallback = names.getFirst();
            log.error(
                "A trust onboarding submission with multiple names detected when mapping it to task. It contained the following languages {}. " +
                    "The correspondence language is {} but it is not within the provided names. As fallback {} is taken. This should not happen and be fixed in Portal.",
                entityNames.keySet(),
                correspondenceLanguage,
                fallback.getKey()
            );
            return null;
        }
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
        var partnerType = submission.getBusinessPartnerType();
        if (partnerType == null) {
            return BusinessPartnerTypeDto.UNKNOWN;
        }
        return switch (partnerType) {
            case BUSINESS -> BusinessPartnerTypeDto.BUSINESS;
            case INDIVIDUAL -> BusinessPartnerTypeDto.INDIVIDUAL;
            case GOVERNMENTAL_INSTITUTION -> BusinessPartnerTypeDto.GOVERNMENTAL_INSTITUTION;
            case UNKNOWN -> BusinessPartnerTypeDto.UNKNOWN;
        };
    }

    private static Map<TrustOnboardingTaskDto.LanguageDto, String> toNameLanguageDtoMap(MultiLanguageTextDto source) {
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

    private static List<TrustOnboardingTaskDto.ContactDto> toContactDto(TrustOnboardingSubmissionDto source) {
        var contacts = new ArrayList<TrustOnboardingTaskDto.ContactDto>();
        if (source == null) {
            return emptyList();
        }

        contacts.add(
            new TrustOnboardingTaskDto.ContactDto(
                "%s %s".formatted(source.getContactPerson().getFirstName(), source.getContactPerson().getLastName()),
                TrustOnboardingTaskContactTypeDto.CONTACT_PERSON,
                source.getContactPerson().getPhone(),
                source.getContactPerson().getEmail()
            )
        );

        if (source.getSignatories() != null) {
            source
                .getSignatories()
                .stream()
                .filter(Objects::nonNull)
                .forEach(signatory ->
                    contacts.add(
                        new TrustOnboardingTaskDto.ContactDto(
                            "%s %s".formatted(signatory.getFirstName(), signatory.getLastName()),
                            TrustOnboardingTaskContactTypeDto.AUTHORISED_SIGNATORY,
                            signatory.getPhone(),
                            signatory.getEmail()
                        )
                    )
                );
        }

        return contacts;
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

    static TrustOnboardingTaskStatusDto toTrustOnboardingTaskStatusDto(TrustTaskStatus source) {
        return switch (source) {
            case REJECTED -> TrustOnboardingTaskStatusDto.REJECTED;
            case ACCEPTED -> TrustOnboardingTaskStatusDto.ACCEPTED;
            case OPENED -> TrustOnboardingTaskStatusDto.OPENED;
            case INFORMATION_REQUESTED -> TrustOnboardingTaskStatusDto.INFORMATION_REQUESTED;
        };
    }
}
