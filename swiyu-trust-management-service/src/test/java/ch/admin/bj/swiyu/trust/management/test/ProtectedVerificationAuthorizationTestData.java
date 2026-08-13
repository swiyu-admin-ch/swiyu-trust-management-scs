package ch.admin.bj.swiyu.trust.management.test;

import ch.admin.bj.swiyu.trust.management.modules.management.api.AuthorizableFieldDto;
import ch.admin.bj.swiyu.trust.management.modules.management.api.ProtectedVerificationAuthorizationRequestDto;
import ch.admin.bj.swiyu.trust.management.modules.management.domain.ProtectedVerificationAuthorization;
import ch.admin.bj.swiyu.trust.management.modules.management.domain.ProtectedVerificationField;
import java.util.UUID;
import lombok.experimental.UtilityClass;

@UtilityClass
public class ProtectedVerificationAuthorizationTestData {

    public static ProtectedVerificationAuthorizationRequestDto createProtectedVerificationAuthorizationRequest(
        UUID bpiId
    ) {
        return new ProtectedVerificationAuthorizationRequestDto(bpiId, AuthorizableFieldDto.AHV_NUMBER);
    }

    public static ProtectedVerificationAuthorization defaultProtectedVerificationAuthorization(
        UUID businessPartnerIdentityId
    ) {
        return new ProtectedVerificationAuthorization(
            UUID.randomUUID(),
            businessPartnerIdentityId,
            ProtectedVerificationField.AHV_NUMBER
        );
    }
}
