package ch.admin.bj.swiyu.trust.management.test;

import ch.admin.bj.swiyu.trust.management.modules.management.api.*;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import lombok.experimental.UtilityClass;

@UtilityClass
public class RequestTestData {

    public static final String SUBJECT_A =
        "did:tdw:DEADBEEF0000000000000000000000000000000000000000000000000000000000000000000000000000000000000:test-identifier-data.service:api:v1:did:00000000-0000-0000-0000-000000000000";
    public static final ObjectMapper objectMapper = new ObjectMapper();

    public static ObjectNode tsMetadataV1Request(
        Map<String, String> orgName,
        Map<String, String> logoUri,
        String preferredLanguage
    ) {
        return tsMetadataV1Request(orgName, logoUri, preferredLanguage, null, null, null);
    }

    public static ObjectNode tsMetadataV1Request(
        Map<String, String> orgName,
        Map<String, String> logoUri,
        String preferredLanguage,
        String subject,
        Instant validFrom,
        Instant validUntil
    ) {
        if (subject == null) subject = SUBJECT_A;
        if (validFrom == null) validFrom = Instant.now();
        if (validUntil == null) validUntil = validFrom.plus(Duration.ofMinutes(1));

        var dto = objectMapper.createObjectNode();
        dto.put("subject", subject);
        dto.put("validFrom", validFrom.toString());
        dto.put("validUntil", validUntil.toString());
        dto.put("preferredLanguage", preferredLanguage);
        dto.set("orgName", objectMapper.valueToTree(orgName));
        if (logoUri != null) dto.set("logoUri", objectMapper.valueToTree(logoUri));
        return dto;
    }

    public static TrustStatementPartnerLinkMetadataV1RequestDto tsMetadataV1RequestDto() {
        return new TrustStatementPartnerLinkMetadataV1RequestDto(
            SUBJECT_A,
            Instant.now(),
            Instant.now().plus(Duration.ofDays(365)),
            Map.of(
                TrustStatementPartnerLinkMetadataV1RequestDto.LanguageDto.EN,
                "Example Organization",
                TrustStatementPartnerLinkMetadataV1RequestDto.LanguageDto.DE_CH,
                "Beispielorganisation",
                TrustStatementPartnerLinkMetadataV1RequestDto.LanguageDto.FR_CH,
                "Exemple d'organisation",
                TrustStatementPartnerLinkMetadataV1RequestDto.LanguageDto.IT_CH,
                "Organizzazione di esempio",
                TrustStatementPartnerLinkMetadataV1RequestDto.LanguageDto.RM_CH,
                "organisaziun exemplarica"
            ),
            Map.of(
                TrustStatementPartnerLinkMetadataV1RequestDto.LanguageDto.EN,
                "data:image/png;base64,abc",
                TrustStatementPartnerLinkMetadataV1RequestDto.LanguageDto.DE_CH,
                "data:image/png;base64,abc"
            ),
            TrustStatementPartnerLinkMetadataV1RequestDto.LanguageDto.DE_CH
        );
    }

    public static TrustStatementPartnerLinkIdentityV1RequestDto tsIdentityV1RequestDto() {
        return new TrustStatementPartnerLinkIdentityV1RequestDto(
            SUBJECT_A,
            Instant.now(),
            Instant.now().plus(Duration.ofDays(365)),
            Map.of(
                TrustStatementPartnerLinkIdentityV1RequestDto.LanguageDto.EN,
                "Example Organization",
                TrustStatementPartnerLinkIdentityV1RequestDto.LanguageDto.DE_CH,
                "Beispielorganisation",
                TrustStatementPartnerLinkIdentityV1RequestDto.LanguageDto.FR_CH,
                "Exemple d'organisation",
                TrustStatementPartnerLinkIdentityV1RequestDto.LanguageDto.IT_CH,
                "Organizzazione di esempio",
                TrustStatementPartnerLinkIdentityV1RequestDto.LanguageDto.RM_CH,
                "organisaziun exemplarica"
            ),
            true,
            List.of(
                new TrustStatementPartnerLinkIdentityV1RequestDto.RegistryIdDto("UID", "CHE-000.000.000"),
                new TrustStatementPartnerLinkIdentityV1RequestDto.RegistryIdDto("LEI", "0A1B2C3D4E5F6G7H8J9I")
            )
        );
    }

    public static TrustStatementPartnerLinkIssuanceV1RequestDto tsIssuanceV1RequestDto() {
        return new TrustStatementPartnerLinkIssuanceV1RequestDto(
            SUBJECT_A,
            Instant.now(),
            Instant.now().plus(Duration.ofDays(365)),
            "https://trust-reg.trust-infra.swiyu.admin.ch/api/v1/vc-schema/test-vc-schema"
        );
    }

    public static TrustStatementPartnerLinkVerificationV1RequestDto tsVerificationV1RequestDto() {
        return new TrustStatementPartnerLinkVerificationV1RequestDto(
            SUBJECT_A,
            Instant.now(),
            Instant.now().plus(Duration.ofDays(365)),
            "https://trust-reg.trust-infra.swiyu.admin.ch/api/v1/vc-schema/test-vc-schema"
        );
    }

    public static String logoUri() {
        return "data:image/png;base64,iVBORw0KGgoAAAANSUhEUgAAAFwAAABcCAMAAADUMSJqAAAAGFBMVEX/AAD/////w8P/39//rKz/4+P/v7//6+th/ykyAAAAb0lEQVRoge3YQQrAIAwFUWtMvP+Nu250EcQUamfW8pb5YClERERbaiqPtG3E6+Wq4ODg4ODgb+LmcVuEWh2y7vFu46vIrqqHomkAl1VcwMHBwU/EU09u6ljM2jZzs767/uDg4ODgZ+Cpn5ZERPTrbtr5CF36/dVBAAAAAElFTkSuQmCC";
    }
}
