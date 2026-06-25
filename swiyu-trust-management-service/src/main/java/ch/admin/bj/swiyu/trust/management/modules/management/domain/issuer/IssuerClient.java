package ch.admin.bj.swiyu.trust.management.modules.management.domain.issuer;

import ch.admin.bj.swiyu.trust.client.issuer.management.model.CreateCredentialRequestDto;
import ch.admin.bj.swiyu.trust.client.issuer.management.model.CredentialStatusTypeDto;
import java.util.UUID;

public interface IssuerClient {
    void updateCredentialStatus(UUID credentialId, CredentialStatusTypeDto credentialStatus);
    TrustStatementIssuanceResult issueTrustStatement(CreateCredentialRequestDto credentialOfferRequest);
    CredentialStatusTypeDto getCredentialStatus(UUID trustIssuerManagementId);
    String getStatusListUri();

    /**
     * Response as it comes from oid4vci API
     * Required due to lack of documentation in oid4vci component
     */
    record CredentialResponseDto(String credential, String format) {}

    /**
     * Holds the result of the trust statement issuance process.
     *
     * @param managementId the id of the credential offer (management id of the issuer credential)
     * @param encodedVc    the encoded verifiable credential as it can be published to the trust registry
     */
    record TrustStatementIssuanceResult(UUID managementId, String encodedVc) {}
}
