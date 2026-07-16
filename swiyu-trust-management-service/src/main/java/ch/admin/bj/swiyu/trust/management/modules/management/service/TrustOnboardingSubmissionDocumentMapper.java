package ch.admin.bj.swiyu.trust.management.modules.management.service;

import ch.admin.bj.swiyu.trust.management.modules.management.api.TrustOnboardingSubmissionDocumentListItemDto;
import lombok.experimental.UtilityClass;

@UtilityClass
public class TrustOnboardingSubmissionDocumentMapper {

    public static TrustOnboardingSubmissionDocumentListItemDto toTrustOnboardingSubmissionDocumentListItemDto(
        ch.admin.bj.swiyu.trust.client.core.business.internal.model.TrustOnboardingSubmissionDocumentListItemDto document
    ) {
        return new TrustOnboardingSubmissionDocumentListItemDto(
            document.getId(),
            document.getName(),
            document.getMediaType(),
            document.getType().getValue(),
            document.getOwningBusinessPartner(),
            document.getCreatedAt(),
            document.getUpdatedAt(),
            document.getSubmittedAt(),
            document.getTrustOnboardingSubmissionId()
        );
    }
}
