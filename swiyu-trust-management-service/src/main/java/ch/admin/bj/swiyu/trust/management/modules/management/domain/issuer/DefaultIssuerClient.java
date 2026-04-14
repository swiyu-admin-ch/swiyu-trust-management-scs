package ch.admin.bj.swiyu.trust.management.modules.management.domain.issuer;

import ch.admin.bj.swiyu.trust.client.issuer.management.api.CredentialApi;
import ch.admin.bj.swiyu.trust.client.issuer.management.model.CreateCredentialRequestDto;
import ch.admin.bj.swiyu.trust.client.issuer.management.model.CredentialStatusTypeDto;
import ch.admin.bj.swiyu.trust.client.issuer.oid4vci.api.IssuerOid4VciApi;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.UUID;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.loader.net.util.UrlDecoder;
import org.springframework.web.util.UriComponentsBuilder;

@Slf4j
@AllArgsConstructor
public class DefaultIssuerClient implements IssuerClient {

    private final CredentialApi issuerManagementApi;
    private final IssuerOid4VciApi issuerOid4VciAPI;
    private final ObjectMapper jsonMapper;
    private final IssuerProperties issuerProperties;

    @Override
    public void updateCredentialStatus(UUID credentialId, CredentialStatusTypeDto credentialStatus) {
        log.debug("Update credential {} status to {}", credentialId, credentialStatus);
        var result = this.issuerManagementApi.updateCredentialStatus(credentialId, credentialStatus);
        log.debug("Updated credential status is {}", result.getStatus());
    }

    @Override
    public TrustStatementIssuanceResult issueTrustStatement(CreateCredentialRequestDto credentialOfferRequest) {
        // 1/4: create new credential offer
        log.debug("Issuance: creating credential offer ...");
        logCredentialRequestAsJson(credentialOfferRequest);
        var credentialOfferResult = this.issuerManagementApi.createCredential(credentialOfferRequest);

        // 2/4: extract preAuthorizedCode
        log.debug("Issuance: extract pre-authorization code from credential offer ...");

        if (credentialOfferResult.getOfferDeeplink() == null || credentialOfferResult.getOfferDeeplink().isEmpty()) {
            throw new IssuanceException(
                "Failed to extract pre-authorization code from credential offer. Deeplink is empty/null."
            );
        }
        var offerParameters = UriComponentsBuilder.fromUriString(credentialOfferResult.getOfferDeeplink())
            .build()
            .getQueryParams();
        var credentialOfferJson = UrlDecoder.decode(offerParameters.get("credential_offer").getFirst());
        String preAuthorizedCode;
        try {
            preAuthorizedCode = jsonMapper
                .readTree(credentialOfferJson)
                .get("grants")
                .get("urn:ietf:params:oauth:grant-type:pre-authorized_code")
                .get("pre-authorized_code")
                .asText();
        } catch (JsonProcessingException e) {
            throw new IssuanceException("Failed to extract pre-authorization code from credential offer.", e);
        }

        // 3/4: get oauth token to create a new credential in step 3
        log.debug("Issuance: looking up access token to create credential ...");
        var accessToken = this.issuerOid4VciAPI.oauthAccessToken(
            preAuthorizedCode,
            "urn:ietf:params:oauth:grant-type:pre-authorized_code"
        );

        // 4/4: create new credential
        log.debug("Issuance: creating credential ...");
        var authorization = "%s %s".formatted(accessToken.getTokenType(), accessToken.getAccessToken());
        var credentialRequest = new ch.admin.bj.swiyu.trust.client.issuer.oid4vci.model.CredentialRequestDto();
        credentialRequest.setFormat("vc+sd-jwt");
        var result = this.issuerOid4VciAPI.createCredential(authorization, credentialRequest);
        var response = parseCredentialResponse(result);

        return new TrustStatementIssuanceResult(credentialOfferResult.getManagementId(), response.credential());
    }

    @Override
    public CredentialStatusTypeDto getCredentialStatus(UUID trustIssuerManagementId) {
        var response = this.issuerManagementApi.getCredentialStatus(trustIssuerManagementId);
        log.debug("Remote credential status is {}", response.getStatus());
        return response.getStatus();
    }

    @Override
    public String getStatusListUri() {
        return issuerProperties.statusListUri();
    }

    /**
     * Issuer API returns JSON as string, that's why it needs to be parsed manually.
     */
    private CredentialResponseDto parseCredentialResponse(String result) {
        CredentialResponseDto response;
        try {
            response = jsonMapper.readValue(result, CredentialResponseDto.class);
        } catch (JsonProcessingException e) {
            throw new IllegalArgumentException("Invalid CredentialResponse from oid4vci API ", e);
        }
        return response;
    }

    private void logCredentialRequestAsJson(CreateCredentialRequestDto request) {
        try {
            var json = this.jsonMapper.writeValueAsString(request);
            log.trace("Issuance: credential request {}", json);
        } catch (JsonProcessingException e) {
            log.error("failed to serialize CreateCredentialRequest to json", e);
        }
    }
}
