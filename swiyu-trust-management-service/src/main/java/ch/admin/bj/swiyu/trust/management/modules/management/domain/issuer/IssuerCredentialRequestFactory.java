package ch.admin.bj.swiyu.trust.management.modules.management.domain.issuer;

import static org.springframework.util.CollectionUtils.*;

import ch.admin.bj.swiyu.trust.client.issuer.management.model.*;
import ch.admin.bj.swiyu.trust.management.modules.management.domain.*;
import ch.admin.bj.swiyu.trust.management.modules.management.domain.details.*;
import jakarta.validation.constraints.*;
import java.time.temporal.*;
import java.util.*;
import lombok.experimental.*;

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
        return switch (statement.getDetails()) {
            case MetadataV1Details details -> credentialSubjectDataMetadataV1(statement, details);
            case IdentityV1Details details -> credentialSubjectDataIdentityV1(statement, details);
            case IssuanceV1Details details -> credentialSubjectDataIssuanceV1(statement, details);
            case VerificationV1Details details -> credentialSubjectDataVerificationV1(statement, details);
        };
    }

    private static Map<String, Object> credentialSubjectDataMetadataV1(
        TrustStatementPartnerLink statement,
        MetadataV1Details details
    ) {
        var map = new HashMap<>(
            Map.of(
                "sub",
                statement.getSubject(),
                "vct",
                statement.getVct(),
                "prefLang",
                details.getPreferredLanguage(),
                "orgName",
                details.getOrgName()
            )
        );
        if (details.hasAnyLogoUri()) {
            map.put("logoUri", details.getLogoUri());
        }
        return map;
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
