package ch.admin.bj.swiyu.trust.management.modules.management.service;

import static ch.admin.bj.swiyu.trust.client.core.business.internal.model.BusinessPartnerTypeDto.BUSINESS;
import static java.util.Collections.emptyList;
import static org.springframework.util.CollectionUtils.isEmpty;

import ch.admin.bj.swiyu.trust.client.core.business.internal.model.*;
import ch.admin.bj.swiyu.trust.client.zas.sbn.model.NullableTranslationDto;
import ch.admin.bj.swiyu.trust.client.zas.sbn.model.OrganisationDto;
import ch.admin.bj.swiyu.trust.client.zas.sbn.model.TranslationDto;
import ch.admin.bj.swiyu.trust.client.zas.sbn.model.UsnDto;
import ch.admin.bj.swiyu.trust.management.modules.management.api.*;
import ch.admin.bj.swiyu.trust.management.modules.management.api.BusinessPartnerTypeDto;
import ch.admin.bj.swiyu.trust.management.modules.management.api.task.*;
import ch.admin.bj.swiyu.trust.management.modules.management.api.task.taskaction.TaskActionDto;
import ch.admin.bj.swiyu.trust.management.modules.management.domain.task.ProtectedVerificationRequestTask;
import ch.admin.bj.swiyu.trust.management.modules.management.domain.task.Task;
import ch.admin.bj.swiyu.trust.management.modules.management.domain.task.TaskStatus;
import ch.admin.bj.swiyu.trust.management.modules.management.domain.task.TrustOnboardingTask;
import java.util.*;
import java.util.stream.Collectors;
import lombok.experimental.UtilityClass;

@UtilityClass
public class TaskMapper {

    public static TaskListItemDto toTaskListItemDto(Task task, Set<TaskActionDto> allowedActions) {
        return new TaskListItemDto(
            task.getId(),
            task.getPartnerName(),
            task.getSubmittedAt(),
            task.getDueAt(),
            toTaskStatusDto(task.getStatus()),
            task.getAssignee(),
            task.getTaskType().name(),
            allowedActions
        );
    }

    public static TrustOnboardingTaskDto toTrustOnboardingTaskDto(
        Set<TaskActionDto> allowedActions,
        TrustOnboardingTask task,
        TrustOnboardingSubmissionDto submission
    ) {
        return new TrustOnboardingTaskDto(
            task.getId(),
            task.getAssignee(),
            task.getSubmittedAt(),
            task.getDueAt(),
            toTaskStatusDto(task.getStatus()),
            toBusinessPartnerTypeDto(submission),
            toUid(submission.getRegistryIds()),
            submission.getIsRegisteredInCommercialRegister(),
            toEntityName(submission.getName()),
            toAddressStreet(submission.getAddress()),
            toAddressZipCodeCity(submission.getAddress()),
            toAddressCountry(submission.getAddress()),
            submission.getEntityEmail(),
            toCorrespondanceLanguageDto(
                submission.getContactPerson() == null ? null : submission.getContactPerson().getCorrespondingLanguage()
            ),
            toContactDto(submission),
            toDidDto(submission.getProofOfPossessions()),
            allowedActions,
            task.getTimesResubmitted()
        );
    }

    public static ProtectedVerificationRequestTaskDto toProtectedVerificationRequestTaskDto(
        ProtectedVerificationRequestTask task,
        ProtectedVerificationSubmissionDto submission,
        Set<TaskActionDto> allowedActions
    ) {
        return new ProtectedVerificationRequestTaskDto(
            task.getId(),
            task.getAssignee(),
            task.getSubmittedAt(),
            task.getDueAt(),
            toTaskStatusDto(task.getStatus()),
            task.getPartnerName(),
            task.getPartnerId(),
            task.getProtectedVerificationSubmissionId(),
            submission.getUid(),
            toContactPersonDto(submission),
            submission.getReason(),
            submission.getSbnId(),
            toAuthorizableField(submission.getCategory()),
            task.hasOpenedZasData(),
            allowedActions
        );
    }

    public static AuthorizableFieldDto toAuthorizableField(ProtectedVerificationCategoryDto category) {
        return switch (category) {
            case PERSONAL_ADMINISTRATIVE_NUMBER -> AuthorizableFieldDto.AHV_NUMBER;
        };
    }

    private static ProtectedVerificationRequestTaskDto.ContactPersonDto toContactPersonDto(
        ProtectedVerificationSubmissionDto submission
    ) {
        var contact = submission.getContactPerson();
        if (contact == null) {
            return null;
        }
        return new ProtectedVerificationRequestTaskDto.ContactPersonDto(
            contact.getFirstName(),
            contact.getLastName(),
            contact.getEmail(),
            contact.getPhone()
        );
    }

    public static ZasDataDto toZasDataDto(UsnDto usn) {
        return new ZasDataDto(
            usn.getBusinessId(),
            toOrganisationDto(usn.getOrganisation()),
            usn.getStatus() != null ? usn.getStatus().getCode() : null,
            usn.getStatus() != null ? toTranslationMap(usn.getStatus().getTranslation()) : Map.of(),
            usn.getCommissionedOrganisationId(),
            usn.getActivityDomain() != null ? usn.getActivityDomain().getCode() : null,
            usn.getActivityDomain() != null
                ? toActivityDomainTranslationMap(usn.getActivityDomain().getTranslation())
                : Map.of()
        );
    }

    private static ZasDataDto.OrganisationDto toOrganisationDto(OrganisationDto organisation) {
        if (organisation == null) {
            return null;
        }
        return new ZasDataDto.OrganisationDto(
            organisation.getCanton() != null ? organisation.getCanton().getCode() : null,
            organisation.getIde(),
            organisation.getName(),
            organisation.getPhoneNumber(),
            organisation.getStreetName(),
            organisation.getStreetNumber(),
            organisation.getPoBox(),
            organisation.getNpa(),
            organisation.getLocality()
        );
    }

    private static Map<String, String> toTranslationMap(NullableTranslationDto translation) {
        if (translation == null) {
            return Map.of();
        }
        return toTranslationMap(translation.getLabelDe(), translation.getLabelFr(), translation.getLabelIt());
    }

    private static Map<String, String> toActivityDomainTranslationMap(TranslationDto translation) {
        if (translation == null) {
            return Map.of();
        }
        return toTranslationMap(translation.getLabelDe(), translation.getLabelFr(), translation.getLabelIt());
    }

    private static Map<String, String> toTranslationMap(String de, String fr, String it) {
        var map = new LinkedHashMap<String, String>();
        if (de != null) {
            map.put("de-CH", de);
        }
        if (fr != null) {
            map.put("fr-CH", fr);
        }
        if (it != null) {
            map.put("it-CH", it);
        }
        return Map.copyOf(map);
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
            throw new IllegalArgumentException("Submission should never have null business partner type");
        }
        return switch (partnerType) {
            case BUSINESS -> BusinessPartnerTypeDto.BUSINESS;
            case INDIVIDUAL -> BusinessPartnerTypeDto.INDIVIDUAL;
            case GOVERNMENTAL_INSTITUTION -> BusinessPartnerTypeDto.GOVERNMENTAL_INSTITUTION;
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

    static TaskStatusDto toTaskStatusDto(TaskStatus source) {
        return switch (source) {
            case REJECTED -> TaskStatusDto.REJECTED;
            case ACCEPTED -> TaskStatusDto.ACCEPTED;
            case OPENED -> TaskStatusDto.OPENED;
            case INFORMATION_REQUESTED -> TaskStatusDto.INFORMATION_REQUESTED;
            case RESUBMITTED -> TaskStatusDto.RESUBMITTED;
        };
    }
}
