package ch.admin.bj.swiyu.trust.management.modules.management.domain.issuer;

import static org.springframework.util.CollectionUtils.isEmpty;

import ch.admin.bj.swiyu.trust.client.issuer.management.model.CreateCredentialRequestDto;
import ch.admin.bj.swiyu.trust.management.modules.management.domain.TrustStatementPartnerLink;
import ch.admin.bj.swiyu.trust.management.modules.management.domain.details.*;
import jakarta.validation.constraints.NotNull;
import java.time.temporal.ChronoUnit;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import lombok.experimental.UtilityClass;

/**
 * Creates a {@link CreateCredentialRequestDto} for the issuing trust statements at the governmental trust issuer service.
 * Each trust statement type has its own required details (claims) and the request is built accordingly.
 */
@UtilityClass
public class IssuerCredentialRequestFactory {

    public static CreateCredentialRequestDto createIssuerCredentialRequest(
        TrustStatementPartnerLink statement,
        @NotNull List<String> statusLists
    ) {
        var request = new CreateCredentialRequestDto();
        request.setCredentialValidFrom(statement.getValidFrom().truncatedTo(ChronoUnit.SECONDS));
        request.setCredentialValidUntil(statement.getValidUntil().truncatedTo(ChronoUnit.SECONDS));
        request.setCredentialSubjectData(statement.getSubject());
        request.setMetadataCredentialSupportedId(statement.getMetadataCredentialSupportedIds());
        request.setStatusLists(statusLists);
        request.setCredentialSubjectData(credentialSubjectData(statement));
        return request;
    }

    private static Map<String, Object> credentialSubjectData(TrustStatementPartnerLink statement) {
        var errorMsg = "Credential of type %s cannot be requested via external issuer.";
        return switch (statement.getDetails()) {
            case IdentityV1Details details -> credentialSubjectDataIdentityV1(statement, details);
            case IssuanceV1Details details -> credentialSubjectDataIssuanceV1(statement, details);
            case VerificationV1Details details -> credentialSubjectDataVerificationV1(statement, details);
            case IdentityV2Details details -> throw new IllegalArgumentException(errorMsg.formatted(details.getType()));
            case ProtectedVerificationAuthorizationV2Details details -> throw new IllegalArgumentException(
                errorMsg.formatted(details.getType())
            );
            case ProtectedIssuanceAuthorizationV2Details details -> throw new IllegalArgumentException(
                errorMsg.formatted(details.getType())
            );
            case NonComplianceV2Details details -> throw new IllegalArgumentException(
                errorMsg.formatted(details.getType())
            );
            case ProtectedIssuanceV2Details details -> throw new IllegalArgumentException(
                errorMsg.formatted(details.getType())
            );
            case VerificationQueryV2Details details -> throw new IllegalArgumentException(
                errorMsg.formatted(details.getType())
            );
        };
    }

    private static Map<String, Object> credentialSubjectDataIdentityV1(
        TrustStatementPartnerLink statement,
        IdentityV1Details details
    ) {
        var map = new HashMap<>(
            Map.of(
                "sub",
                statement.getSubject(),
                "vct",
                statement.getVct(),
                "entityName",
                details.getEntityName(),
                "isStateActor",
                details.getIsStateActor()
            )
        );
        // optional claims
        if (!isEmpty(details.getRegistryIds())) {
            map.put("registryIds", details.getRegistryIds());
        }
        return map;
    }

    private static Map<String, Object> credentialSubjectDataVerificationV1(
        TrustStatementPartnerLink statement,
        VerificationV1Details details
    ) {
        return Map.of("sub", statement.getSubject(), "vct", statement.getVct(), "canVerify", details.getCanVerify());
    }

    private static Map<String, Object> credentialSubjectDataIssuanceV1(
        TrustStatementPartnerLink statement,
        IssuanceV1Details details
    ) {
        return Map.of("sub", statement.getSubject(), "vct", statement.getVct(), "canIssue", details.getCanIssue());
    }
}
