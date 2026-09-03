package ch.admin.bj.swiyu.trust.management.modules.management.domain;

import ch.admin.bj.swiyu.trust.client.core.business.internal.model.BusinessPartnerTypeDto;
import ch.admin.bj.swiyu.trust.client.core.business.internal.model.ProofOfPossessionDto;
import ch.admin.bj.swiyu.trust.client.core.business.internal.model.TrustOnboardingSubmissionDto;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

public record BusinessPartnerIdentityRelevantInformationUpdate(
    Map<String, String> entityName,
    String uid,
    boolean isRegisteredInCommercialRegister,
    String correspondingLanguage,
    Boolean isStateActor,
    @NotNull Set<@NotBlank String> trustedIdentifier
) {
    public static BusinessPartnerIdentityRelevantInformationUpdate of(TrustOnboardingSubmissionDto submission) {
        return new BusinessPartnerIdentityRelevantInformationUpdate(
            Map.copyOf(submission.getName()),
            submission.getRegistryIds().get("UID") == null ? null : submission.getRegistryIds().get("UID"),
            submission.getIsRegisteredInCommercialRegister() != null &&
                submission.getIsRegisteredInCommercialRegister(),
            submission.getCorrespondingLanguage() == null ? null : submission.getCorrespondingLanguage().toString(),
            submission.getBusinessPartnerType() == BusinessPartnerTypeDto.GOVERNMENTAL_INSTITUTION,
            submission.getProofOfPossessions().stream().map(ProofOfPossessionDto::getDid).collect(Collectors.toSet())
        );
    }
}
