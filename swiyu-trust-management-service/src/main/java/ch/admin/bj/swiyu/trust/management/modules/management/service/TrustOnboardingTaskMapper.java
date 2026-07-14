package ch.admin.bj.swiyu.trust.management.modules.management.service;

import static java.util.Collections.emptyList;
import static org.springframework.util.CollectionUtils.isEmpty;

import ch.admin.bj.swiyu.trust.client.core.business.internal.model.*;
import ch.admin.bj.swiyu.trust.management.modules.management.api.TrustOnboardingTaskActionDto;
import ch.admin.bj.swiyu.trust.management.modules.management.domain.TrustOnboardingTask;
import ch.admin.bj.swiyu.trust.management.modules.management.domain.TrustTask;
import ch.admin.bj.swiyu.trust.management.modules.management.domain.TrustTaskStatus;
import ch.admin.bj.swiyu.trust.management.modules.ui.api.BusinessPartnerTypeDto;
import ch.admin.bj.swiyu.trust.management.modules.ui.api.TrustOnboardingTaskContactTypeDto;
import ch.admin.bj.swiyu.trust.management.modules.ui.api.TrustOnboardingTaskDto;
import ch.admin.bj.swiyu.trust.management.modules.ui.api.TrustOnboardingTaskListItemDto;
import ch.admin.bj.swiyu.trust.management.modules.ui.api.TrustOnboardingTaskStatusDto;
import java.util.*;
import java.util.stream.Collectors;
import lombok.experimental.UtilityClass;

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
            task.getPartnerName(),
            task.getSubmittedAt(),
            task.getDueAt(),
            toTrustOnboardingTaskStatusDto(task.getStatus()),
            task.getAssignee(),
            task.getTaskType().name(),
            allowedActions
        );
    }

    public static TrustOnboardingTaskDto toTrustOnboardingTaskDto(
        Set<TrustOnboardingTaskActionDto> allowedActions,
        TrustOnboardingTask task,
        TrustOnboardingSubmissionDto submission
    ) {
        return new TrustOnboardingTaskDto(
            task.getId(),
            task.getAssignee(),
            task.getSubmittedAt(),
            task.getDueAt(),
            toTrustOnboardingTaskStatusDto(task.getStatus()),
            toBusinessPartnerTypeDto(submission),
            toUid(submission.getRegistryIds()),
            submission.getIsRegisteredInCommercialRegister(),
            toEntityName(submission.getName()),
            toAddressStreet(submission.getAddress()),
            toAddressZipCodeCity(submission.getAddress()),
            toAddressCountry(submission.getAddress()),
            submission.getEntityEmail(),
            toCorrespondanceLanguageDto(submission.getCorrespondingLanguage()),
            toContactDto(submission),
            toDidDto(submission.getProofOfPossessions()),
            allowedActions
        );
    }

    private static Map<String, String> toEntityName(Map<String, String> name) {
        if (isEmpty(name)) {
            return Map.of();
        }
        return name
            .entrySet()
            .stream()
            .filter(e -> e.getValue() != null)
            .collect(Collectors.toUnmodifiableMap(Map.Entry::getKey, Map.Entry::getValue));
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

    @SuppressWarnings("java:S5738") // EID-6303
    private static TrustOnboardingTaskDto.LanguageDto toCorrespondanceLanguageDto(LanguageDto source) {
        if (source == null) {
            return TrustOnboardingTaskDto.LanguageDto.EN_CH;
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
