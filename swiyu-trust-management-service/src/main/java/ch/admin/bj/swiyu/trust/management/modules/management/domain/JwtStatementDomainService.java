package ch.admin.bj.swiyu.trust.management.modules.management.domain;

import static ch.admin.bj.swiyu.trust.management.modules.common.date.DateTimeHelper.today;

import ch.admin.bj.swiyu.trust.management.modules.common.exception.IssuanceException;
import ch.admin.bj.swiyu.trust.management.modules.management.config.issuer.SignerContext;
import ch.admin.bj.swiyu.trust.management.modules.management.config.statements.DefaultStatementProperties;
import ch.admin.bj.swiyu.trust.management.modules.management.domain.details.*;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.nimbusds.jose.*;
import com.nimbusds.jwt.JWTClaimsSet;
import com.nimbusds.jwt.SignedJWT;
import java.time.Instant;
import java.time.format.DateTimeFormatter;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class JwtStatementDomainService {

    private static final String PROFILE_VERSION = "profile_version";
    private static final String ILLEGAL_ARGUMENT_EXCEPTION_TYPE_MISMATCH =
        "TrustStatementPartnerLink of type %s, not as expected of type %s.";

    private final SignerContext trustIssuer;
    private final SignerContext publicTransparencyIssuer;
    private final ObjectMapper objectMapper;
    private final DefaultStatementProperties defaultStatementProperties;

    public String generateIdentityTrustStatement(
        TrustStatementPartnerLink partnerLink,
        StatusListMetadata statusListMetadata
    ) {
        if (partnerLink.getType() != TrustStatementPartnerLinkType.TRUST_STATEMENT_IDENTITY_V2) {
            throw new IllegalArgumentException(
                ILLEGAL_ARGUMENT_EXCEPTION_TYPE_MISMATCH.formatted(
                    partnerLink.getType(),
                    TrustStatementPartnerLinkType.TRUST_STATEMENT_IDENTITY_V2
                )
            );
        }
        var details = (IdentityV2Details) partnerLink.getDetails();

        var header = this.trustIssuerStatementHeaderBuilder();
        header.type(new JOSEObjectType(StatementType.IDENTITY_TRUST_STATEMENT_V2.getJwtTyp()));

        var payload = this.payloadBuilderWithDefaults(partnerLink, statusListMetadata);
        payload.claim("jti", partnerLink.getId());
        payload.subject(partnerLink.getSubject());
        payload.claim("is_state_actor", details.getIsStateActor());
        payload.claim("registry_ids", details.getRegistryIds());
        var entityNames = JsonLocalizationSerializer.fromLocalizedIdentityV2DetailsLanguage(
            "entity_name",
            details.getEntityName()
        );
        for (var entityName : entityNames.entrySet()) {
            payload.claim(entityName.getKey(), entityName.getValue());
        }

        return sign(header.build(), payload.build(), trustIssuer.signer()).serialize();
    }

    public String generateProtectedVerificationAuthorizationTrustStatement(
        TrustStatementPartnerLink partnerLink,
        StatusListMetadata statusListMetadata
    ) {
        if (
            partnerLink.getType() !=
            TrustStatementPartnerLinkType.TRUST_STATEMENT_PROTECTED_VERIFICATION_AUTHORIZATION_V2
        ) {
            throw new IllegalArgumentException(
                ILLEGAL_ARGUMENT_EXCEPTION_TYPE_MISMATCH.formatted(
                    partnerLink.getType(),
                    TrustStatementPartnerLinkType.TRUST_STATEMENT_PROTECTED_VERIFICATION_AUTHORIZATION_V2
                )
            );
        }
        var details = (ProtectedVerificationAuthorizationV2Details) partnerLink.getDetails();

        var header = this.trustIssuerStatementHeaderBuilder();
        header.type(
            new JOSEObjectType(StatementType.PROTECTED_VERIFICATION_AUTHORIZATION_TRUST_STATEMENT_V2.getJwtTyp())
        );

        var payload = this.payloadBuilderWithDefaults(partnerLink, statusListMetadata);
        payload.subject(partnerLink.getSubject());
        payload.claim("jti", partnerLink.getId());
        payload.claim(
            "authorized_fields",
            details
                .getAuthorizedFields()
                .stream()
                .map(s -> s.getJsonRepresentation())
                .toList()
        );

        return sign(header.build(), payload.build(), trustIssuer.signer()).serialize();
    }

    public String generateProtectedIssuanceAuthorizationTrustStatement(
        TrustStatementPartnerLink partnerLink,
        StatusListMetadata statusListMetadata
    ) {
        if (
            partnerLink.getType() != TrustStatementPartnerLinkType.TRUST_STATEMENT_PROTECTED_ISSUANCE_AUTHORIZATION_V2
        ) {
            throw new IllegalArgumentException(
                ILLEGAL_ARGUMENT_EXCEPTION_TYPE_MISMATCH.formatted(
                    partnerLink.getType(),
                    TrustStatementPartnerLinkType.TRUST_STATEMENT_PROTECTED_ISSUANCE_AUTHORIZATION_V2
                )
            );
        }
        var details = (ProtectedIssuanceAuthorizationV2Details) partnerLink.getDetails();

        var header = this.trustIssuerStatementHeaderBuilder();
        header.type(new JOSEObjectType(StatementType.PROTECTED_ISSUANCE_AUTHORIZATION_TRUST_STATEMENT_V2.getJwtTyp()));

        var payload = this.payloadBuilderWithDefaults(partnerLink, statusListMetadata);
        payload.subject(partnerLink.getSubject());
        payload.claim("jti", partnerLink.getId());

        var canIssue = new HashMap<String, String>();
        canIssue.put("vct", details.getCanIssue().vct());
        canIssue.putAll(
            JsonLocalizationSerializer.fromLocalizedProtectedIssuanceAuthorizationV2DetailsLanguage(
                "reason",
                details.getCanIssue().reason()
            )
        );
        canIssue.putAll(
            JsonLocalizationSerializer.fromLocalizedProtectedIssuanceAuthorizationV2DetailsLanguage(
                "vct_name",
                details.getCanIssue().vctName()
            )
        );
        payload.claim("can_issue", canIssue);

        return sign(header.build(), payload.build(), trustIssuer.signer()).serialize();
    }

    public String generateNonComplianceTrustListStatement(
        TrustStatementPartnerLink partnerLink,
        StatusListMetadata statusListMetadata
    ) {
        if (partnerLink.getType() != TrustStatementPartnerLinkType.TRUST_LIST_STATEMENT_NON_COMPLIANCE_V2) {
            throw new IllegalArgumentException(
                ILLEGAL_ARGUMENT_EXCEPTION_TYPE_MISMATCH.formatted(
                    partnerLink.getType(),
                    TrustStatementPartnerLinkType.TRUST_LIST_STATEMENT_NON_COMPLIANCE_V2
                )
            );
        }
        var details = (NonComplianceV2Details) partnerLink.getDetails();

        var header = this.trustIssuerStatementHeaderBuilder();
        header.type(new JOSEObjectType(StatementType.NON_COMPLIANCE_TRUST_LIST_STATEMENT_V2.getJwtTyp()));

        var payload = this.payloadBuilderWithDefaults(partnerLink, statusListMetadata);
        payload.claim("jti", partnerLink.getId());
        payload.claim(
            "non_compliant_actors",
            details
                .getNonCompliantActors()
                .stream()
                .map(a -> {
                    var actorMap = new HashMap<String, String>();
                    actorMap.put("actor", a.actor());
                    actorMap.put("flagged_at", DateTimeFormatter.ISO_INSTANT.format(a.flaggedAt()));
                    actorMap.putAll(
                        JsonLocalizationSerializer.fromLocalizedNonComplianceV2DetailsLanguage("reason", a.reason())
                    );
                    return actorMap;
                })
                .toList()
        );

        return sign(header.build(), payload.build(), trustIssuer.signer()).serialize();
    }

    public String generateProtectedIssuanceTrustListStatement(
        TrustStatementPartnerLink partnerLink,
        StatusListMetadata statusListMetadata
    ) {
        if (partnerLink.getType() != TrustStatementPartnerLinkType.TRUST_LIST_STATEMENT_PROTECTED_ISSUANCE_V2) {
            throw new IllegalArgumentException(
                ILLEGAL_ARGUMENT_EXCEPTION_TYPE_MISMATCH.formatted(
                    partnerLink.getType(),
                    TrustStatementPartnerLinkType.TRUST_LIST_STATEMENT_PROTECTED_ISSUANCE_V2
                )
            );
        }
        var details = (ProtectedIssuanceV2Details) partnerLink.getDetails();

        var header = this.trustIssuerStatementHeaderBuilder();
        header.type(new JOSEObjectType(StatementType.PROTECTED_ISSUANCE_TRUST_LIST_STATEMENT_V2.getJwtTyp()));

        var payload = this.payloadBuilderWithDefaults(partnerLink, statusListMetadata);
        payload.claim("jti", partnerLink.getId());
        payload.claim("vct_values", details.getVctValues());

        return sign(header.build(), payload.build(), trustIssuer.signer()).serialize();
    }

    public String generateVerificationQueryPublicStatement(TrustStatementPartnerLink partnerLink) {
        if (partnerLink.getType() != TrustStatementPartnerLinkType.PUBLIC_STATEMENT_VERIFICATION_QUERY_V2) {
            throw new IllegalArgumentException(
                ILLEGAL_ARGUMENT_EXCEPTION_TYPE_MISMATCH.formatted(
                    partnerLink.getType(),
                    TrustStatementPartnerLinkType.PUBLIC_STATEMENT_VERIFICATION_QUERY_V2
                )
            );
        }
        var details = (VerificationQueryV2Details) partnerLink.getDetails();

        var header = this.publicTransparencyIssuerStatementHeaderBuilder();
        header.type(new JOSEObjectType(StatementType.VERIFICATION_QUERY_PUBLIC_STATEMENT_V2.getJwtTyp()));

        var payload = this.payloadBuilderWithDefaults(partnerLink, null);
        payload.subject(partnerLink.getSubject());
        payload.claim("jti", partnerLink.getId());
        payload.claim(
            "request",
            Map.of(
                "type",
                details.getRequest().type(),
                "scope",
                details.getRequest().scope(),
                "query",
                objectMapper.convertValue(details.getRequest().query(), Object.class)
            )
        );

        for (var e : JsonLocalizationSerializer.fromLocalizedVerificationQueryV2DetailsLanguage(
            "purpose_name",
            details.getPurposeName()
        ).entrySet()) {
            payload.claim(e.getKey(), e.getValue());
        }
        for (var e : JsonLocalizationSerializer.fromLocalizedVerificationQueryV2DetailsLanguage(
            "purpose_description",
            details.getPurposeDescription()
        ).entrySet()) {
            payload.claim(e.getKey(), e.getValue());
        }

        return sign(header.build(), payload.build(), publicTransparencyIssuer.signer()).serialize();
    }

    public SignedJWT generateEmptyTrustIssuerToken() {
        var header = this.trustIssuerStatementHeaderBuilder();
        header.type(new JOSEObjectType("test+jwt"));

        var payload = this.payloadBuilderWithDefaults();
        payload.claim("test_at", today().toString());
        return sign(header.build(), payload.build(), trustIssuer.signer());
    }

    public SignedJWT generateEmptyPublicTransparencyIssuerToken() {
        var header = this.publicTransparencyIssuerStatementHeaderBuilder();
        header.type(new JOSEObjectType("test+jwt"));

        var payload = this.payloadBuilderWithDefaults();
        payload.claim("test_at", today().toString());
        return sign(header.build(), payload.build(), publicTransparencyIssuer.signer());
    }

    public SignedJWT generateTokenStatusList(
        StatusListMetadata statusListMetadata,
        List<TrustStatementPartnerLink> revokedStatusListEntries
    ) {
        var header = this.trustIssuerHeaderBuilder();
        header.type(new JOSEObjectType("statuslist+jwt"));
        header.customParam(PROFILE_VERSION, "swiss-profile-vc:1.0.0");

        var payload = this.payloadBuilderWithDefaults()
            .expirationTime(Date.from(today().plus(defaultStatementProperties.statuslist().timeToLive()).toInstant()))
            .subject(statusListMetadata.getStatusRegistryUrl());

        var token = new TokenStatusListToken(TokenStatusListBit.MAX.getValue(), statusListMetadata.getMaxSize());
        for (var statusListEntry : revokedStatusListEntries) {
            if (statusListEntry.getStatus() != TrustStatementPartnerLinkStatus.INACTIVE) {
                throw new IllegalArgumentException("Entries send to generateTokenStatusList must be INACTIVE.");
            }
            token.setStatus(statusListEntry.getStatusListIndex(), TokenStatusListBit.REVOKE.getValue());
        }
        payload.claim("status_list", token.getStatusListClaims());

        return sign(header.build(), payload.build(), trustIssuer.signer());
    }

    private JWSHeader.Builder trustIssuerStatementHeaderBuilder() {
        return trustIssuerHeaderBuilder().customParam(PROFILE_VERSION, "swiss-profile-trust:1.0.0");
    }

    private JWSHeader.Builder trustIssuerHeaderBuilder() {
        return new JWSHeader.Builder(JWSAlgorithm.ES256).keyID(trustIssuer.kid());
    }

    private JWSHeader.Builder publicTransparencyIssuerStatementHeaderBuilder() {
        return new JWSHeader.Builder(JWSAlgorithm.ES256)
            .keyID(publicTransparencyIssuer.kid())
            .customParam(PROFILE_VERSION, "swiss-profile-trust:1.0.0");
    }

    private JWTClaimsSet.Builder payloadBuilderWithDefaults() {
        var now = today();
        return payloadBuilderWithDefaults(
            now.toInstant(),
            now.plus(defaultStatementProperties.timeToLive()).toInstant()
        );
    }

    private JWTClaimsSet.Builder payloadBuilderWithDefaults(
        TrustStatementPartnerLink partnerLink,
        StatusListMetadata statuslistMetadata
    ) {
        if (
            (partnerLink.getStatusListIndex() != null && statuslistMetadata == null) ||
            (partnerLink.getStatusListIndex() == null && statuslistMetadata != null)
        ) {
            throw new IllegalArgumentException(
                "statuslistMetadata and index config do not match. Both need to be set to be valid."
            );
        }

        var builder = payloadBuilderWithDefaults(partnerLink.getValidFrom(), partnerLink.getValidUntil());
        if (partnerLink.getStatusListIndex() != null && statuslistMetadata != null) {
            builder.claim(
                "status",
                Map.of(
                    "status_list",
                    Map.of("idx", partnerLink.getStatusListIndex(), "uri", statuslistMetadata.getStatusRegistryUrl())
                )
            );
        }
        return builder;
    }

    private JWTClaimsSet.Builder payloadBuilderWithDefaults(Instant validFrom, Instant validUntil) {
        return new JWTClaimsSet.Builder()
            .issueTime(Date.from(Instant.now()))
            .notBeforeTime(Date.from(validFrom))
            .expirationTime(Date.from(validUntil));
    }

    private SignedJWT sign(JWSHeader header, JWTClaimsSet payload, JWSSigner signer) {
        var signedJWT = new SignedJWT(header, payload);
        try {
            signedJWT.sign(signer);
        } catch (JOSEException e) {
            throw new IssuanceException("Issuer signing failed", e);
        }
        return signedJWT;
    }
}
