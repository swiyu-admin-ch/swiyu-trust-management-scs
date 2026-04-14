package ch.admin.bj.swiyu.trust.management.modules.management.service;

import ch.admin.bj.swiyu.trust.management.modules.ui.api.TrustOnboardingSubmissionDocumentListItemDto;
import lombok.experimental.*;

@UtilityClass
public class TrustOnboardingSubmissionDocumentMapper {

    public static TrustOnboardingSubmissionDocumentListItemDto toTrustOnboardingSubmissionDocumentListItemDto(
        ch.admin.bj.swiyu.trust.client.core.business.model.TrustOnboardingSubmissionDocumentListItemDto document
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
