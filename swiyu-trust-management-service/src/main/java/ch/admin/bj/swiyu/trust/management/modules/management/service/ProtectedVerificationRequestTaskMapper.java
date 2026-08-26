package ch.admin.bj.swiyu.trust.management.modules.management.service;

import static ch.admin.bj.swiyu.trust.management.modules.management.service.TrustOnboardingTaskMapper.toTrustOnboardingTaskStatusDto;

import ch.admin.bj.swiyu.trust.client.core.business.internal.model.ProtectedVerificationSubmissionDto;
import ch.admin.bj.swiyu.trust.client.zas.sbn.model.NullableTranslationDto;
import ch.admin.bj.swiyu.trust.client.zas.sbn.model.OrganisationDto;
import ch.admin.bj.swiyu.trust.client.zas.sbn.model.TranslationDto;
import ch.admin.bj.swiyu.trust.client.zas.sbn.model.UsnDto;
import ch.admin.bj.swiyu.trust.management.modules.management.api.AuthorizableFieldDto;
import ch.admin.bj.swiyu.trust.management.modules.management.api.ProtectedVerificationRequestTaskDto;
import ch.admin.bj.swiyu.trust.management.modules.management.api.TrustOnboardingTaskActionDto;
import ch.admin.bj.swiyu.trust.management.modules.management.api.ZasDataDto;
import ch.admin.bj.swiyu.trust.management.modules.management.domain.ProtectedVerificationRequestTask;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;
import lombok.experimental.UtilityClass;

@UtilityClass
public class ProtectedVerificationRequestTaskMapper {

    public static ProtectedVerificationRequestTaskDto toProtectedVerificationRequestTaskDto(
        ProtectedVerificationRequestTask task,
        ProtectedVerificationSubmissionDto submission,
        Set<TrustOnboardingTaskActionDto> allowedActions
    ) {
        return new ProtectedVerificationRequestTaskDto(
            task.getId(),
            task.getAssignee(),
            task.getSubmittedAt(),
            task.getDueAt(),
            toTrustOnboardingTaskStatusDto(task.getStatus()),
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

    public static AuthorizableFieldDto toAuthorizableField(
        ch.admin.bj.swiyu.trust.client.core.business.internal.model.ProtectedVerificationCategoryDto category
    ) {
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
}
