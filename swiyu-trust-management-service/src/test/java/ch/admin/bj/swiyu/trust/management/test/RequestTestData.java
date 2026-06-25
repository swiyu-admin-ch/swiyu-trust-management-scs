package ch.admin.bj.swiyu.trust.management.test;

import ch.admin.bj.swiyu.trust.management.modules.management.api.*;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import jakarta.validation.Valid;
import java.time.Duration;
import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import lombok.experimental.UtilityClass;

@UtilityClass
public class RequestTestData {

    public static final String BUSINESS_PARTNER_A_ID_S = "9f425029-9775-4984-99ba-bacc60069502";
    public static final UUID BUSINESS_PARTNER_A_ID = UUID.fromString(BUSINESS_PARTNER_A_ID_S);
    public static final String SUBJECT_A =
        "did:tdw:DEADBEEF0000000000000000000000000000000000000000000000000000000000000000000000000000000000000:test-identifier-data.service:api:v1:did:00000000-0000-0000-0000-000000000000";

    public static final ObjectMapper objectMapper = new ObjectMapper()
        .registerModule(new JavaTimeModule())
        .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);

    public static IdentityV1RequestDto tsIdentityV1RequestDto() {
        return tsIdentityV1RequestDto(SUBJECT_A);
    }

    public static IdentityV1RequestDto tsIdentityV1RequestDto(String subject) {
        return new IdentityV1RequestDto(
            subject,
            Instant.now(),
            Instant.now().plus(Duration.ofDays(365)),
            localizedText(
                "Beispielorganisation",
                Map.of(
                    "en",
                    "Example Organization",
                    "de-CH",
                    "Beispielorganisation",
                    "fr-CH",
                    "Exemple d'organisation",
                    "it-CH",
                    "Organizzazione di esempio",
                    "rm-CH",
                    "organisaziun exemplarica"
                )
            ),
            true,
            List.of(
                new IdentityV1RequestDto.RegistryIdDto("UID", "CHE-000.000.000"),
                new IdentityV1RequestDto.RegistryIdDto("LEI", "0A1B2C3D4E5F6G7H8J9I")
            )
        );
    }

    public static IdentityV2RequestDto tsIdentityV2RequestDto() {
        return tsIdentityV2RequestDto(BUSINESS_PARTNER_A_ID, SUBJECT_A);
    }

    public static IdentityV2RequestDto tsIdentityV2RequestDto(UUID businessPartnerId, String subject) {
        return new IdentityV2RequestDto(
            businessPartnerId,
            subject,
            Instant.now(),
            Instant.now().plus(Duration.ofDays(365)),
            localizedText(
                "Example Organization",
                Map.of(
                    "en",
                    "Example Organization",
                    "de-CH",
                    "Beispielorganisation",
                    "fr-CH",
                    "Exemple d'organisation",
                    "it-CH",
                    "Organizzazione di esempio",
                    "rm-CH",
                    "organisaziun exemplarica"
                )
            ),
            true,
            List.of(
                new IdentityV2RequestDto.RegistryIdDto("UID", "CHE-000.000.000"),
                new IdentityV2RequestDto.RegistryIdDto("LEI", "0A1B2C3D4E5F6G7H8J9I")
            )
        );
    }

    public static IssuanceV1RequestDto tsIssuanceV1RequestDto() {
        return new IssuanceV1RequestDto(
            SUBJECT_A,
            Instant.now(),
            Instant.now().plus(Duration.ofDays(365)),
            "https://trust-reg.trust-infra.swiyu.admin.ch/api/v1/vc-schema/test-vc-schema"
        );
    }

    public static VerificationQueryV2RequestDto tsVerificationQueryV2RequestDto() {
        return new VerificationQueryV2RequestDto(
            BUSINESS_PARTNER_A_ID,
            SUBJECT_A,
            Instant.now(),
            Instant.now().plus(Duration.ofDays(365)),
            Map.of("default", "test-name-default"),
            Map.of("default", "test-description-default"),
            new VerificationQueryV2RequestDto.VerificationRequestObjectDto(
                "DCQL",
                "test",
                objectMapper.createObjectNode()
            )
        );
    }

    public static VerificationV1RequestDto tsVerificationV1RequestDto() {
        return new VerificationV1RequestDto(
            SUBJECT_A,
            Instant.now(),
            Instant.now().plus(Duration.ofDays(365)),
            "https://trust-reg.trust-infra.swiyu.admin.ch/api/v1/vc-schema/test-vc-schema"
        );
    }

    public static String logoUri() {
        return "data:image/png;base64,iVBORw0KGgoAAAANSUhEUgAAAFwAAABcCAMAAADUMSJqAAAAGFBMVEX/AAD/////w8P/39//rKz/4+P/v7//6+th/ykyAAAAb0lEQVRoge3YQQrAIAwFUWtMvP+Nu250EcQUamfW8pb5YClERERbaiqPtG3E6+Wq4ODg4ODgb+LmcVuEWh2y7vFu46vIrqqHomkAl1VcwMHBwU/EU09u6ljM2jZzs767/uDg4ODgZ+Cpn5ZERPTrbtr5CF36/dVBAAAAAElFTkSuQmCC";
    }

    public static ObjectNode tsProtectedVerificationAuthorizationV2Request(
        String businessPartnerId,
        String subject,
        Instant validFrom,
        Instant validUntil
    ) {
        if (businessPartnerId == null) {
            businessPartnerId = BUSINESS_PARTNER_A_ID_S;
        }
        if (subject == null) {
            subject = SUBJECT_A;
        }
        if (validFrom == null) {
            validFrom = Instant.now();
        }
        if (validUntil == null) {
            validUntil = validFrom.plus(Duration.ofMinutes(1));
        }

        var dto = objectMapper.createObjectNode();
        dto.put("subject", subject);
        dto.put("businessPartnerId", businessPartnerId);
        dto.put("validFrom", validFrom.toString());
        dto.put("validUntil", validUntil.toString());
        dto.set("authorizedFields", objectMapper.createArrayNode().add("AHV_NUMBER"));
        return dto;
    }

    public static ObjectNode tsVqpsV2Request(
        String businessPartnerId,
        String subject,
        Instant validFrom,
        Instant validUntil
    ) {
        if (businessPartnerId == null) {
            businessPartnerId = BUSINESS_PARTNER_A_ID_S;
        }
        if (subject == null) {
            subject = SUBJECT_A;
        }
        if (validFrom == null) {
            validFrom = Instant.now();
        }
        if (validUntil == null) {
            validUntil = validFrom.plus(Duration.ofMinutes(1));
        }

        ObjectNode dto = objectMapper.createObjectNode();

        // Simple fields
        dto.put("subject", subject);
        dto.put("validFrom", validFrom.toString());
        dto.put("validUntil", validUntil.toString());
        dto.put("businessPartnerId", businessPartnerId);

        // purposeName
        ObjectNode purposeName = objectMapper.createObjectNode();
        purposeName.put("default", "Dummy validation");
        purposeName.put("en", "Dummy validation");
        purposeName.put("de-CH", "Dummy Validieriung");
        purposeName.put("fr-CH", "Validation factice");
        purposeName.put("it-CH", "Convalida fittizia");
        purposeName.put("rm-CH", "Dummy validation");
        dto.set("purposeName", purposeName);

        // purposeDescription
        ObjectNode purposeDescription = objectMapper.createObjectNode();
        purposeDescription.put("default", "Dummy validation reason");
        purposeDescription.put("en", "Dummy validation reason");
        purposeDescription.put("de-CH", "Grund für die Dummy-Validierung");
        purposeDescription.put("fr-CH", "Raison factice de validation");
        purposeDescription.put("it-CH", "Motivo di convalida fittizio");
        purposeDescription.put("rm-CH", "Dummy validation reason");
        dto.set("purposeDescription", purposeDescription);

        // request
        ObjectNode request = objectMapper.createObjectNode();
        request.put("type", "DCQL");
        request.put("scope", "string");

        // query
        ObjectNode query = objectMapper.createObjectNode();
        ArrayNode credentials = objectMapper.createArrayNode();

        // credential entry
        ObjectNode credential = objectMapper.createObjectNode();
        credential.put("id", "some_identity_credential");
        credential.put("format", "dc+sd-jwt");

        // meta
        ObjectNode meta = objectMapper.createObjectNode();
        ArrayNode vctValues = objectMapper.createArrayNode();
        vctValues.add("https://credentials.example.com/identity_credential");
        meta.set("vct_values", vctValues);
        credential.set("meta", meta);

        // claims
        ArrayNode claims = objectMapper.createArrayNode();

        ObjectNode lastNameClaim = objectMapper.createObjectNode();
        lastNameClaim.set("path", objectMapper.createArrayNode().add("last_name"));
        claims.add(lastNameClaim);

        ObjectNode firstNameClaim = objectMapper.createObjectNode();
        firstNameClaim.set("path", objectMapper.createArrayNode().add("first_name"));
        claims.add(firstNameClaim);

        credential.set("claims", claims);
        credentials.add(credential);

        // assemble
        query.set("credentials", credentials);
        request.set("query", query);
        dto.set("request", request);
        return dto;
    }

    public static ObjectNode tsProtectedIssuanceAuthorizationV2Request(
        String businessPartnerId,
        String subject,
        Instant validFrom,
        Instant validUntil
    ) {
        if (businessPartnerId == null) {
            businessPartnerId = BUSINESS_PARTNER_A_ID_S;
        }
        if (subject == null) {
            subject = SUBJECT_A;
        }
        if (validFrom == null) {
            validFrom = Instant.now();
        }
        if (validUntil == null) {
            validUntil = validFrom.plus(Duration.ofMinutes(1));
        }

        var dto = objectMapper.createObjectNode();
        dto.put("subject", subject);
        dto.put("businessPartnerId", businessPartnerId);
        dto.put("validFrom", validFrom.toString());
        dto.put("validUntil", validUntil.toString());
        var canIssue = objectMapper.createObjectNode();
        canIssue.put("vct", "urn:vct:test");
        canIssue.set(
            "vctName",
            objectMapper.createObjectNode().put("default", "Test issuance").put("en", "Test issuance")
        );
        canIssue.set(
            "reason",
            objectMapper.createObjectNode().put("default", "Test run").put("en", "Test run" + Instant.now())
        );
        dto.set("canIssue", canIssue);
        return dto;
    }

    public static ObjectNode tsProtectedIssuanceEntryCreateRequest(String vct) {
        if (vct == null) {
            vct = "urn:vct:test";
        }

        var dto = objectMapper.createObjectNode();
        dto.put("vct", vct);

        return dto;
    }

    public static ObjectNode tsNonComplianceActorCreateRequest(String did) {
        if (did == null) {
            did = SUBJECT_A;
        }

        var dto = objectMapper.createObjectNode();
        dto.put("did", did);
        ObjectNode reason = objectMapper.createObjectNode();
        reason.put("reasonEn", "Dummy actor");
        reason.put("reasonDe", "Dummy actor");
        reason.put("reasonFr", "Validation actor");
        reason.put("reasonIt", "Convalida actor");
        reason.put("reasonRm", "Dummy actor");
        dto.set("reason", reason);

        return dto;
    }

    public static @Valid ProtectedIssuanceAuthorizationV2RequestDto tsProtectedIssuanceAuthorizationV2RequestDto(
        UUID buisnessPartnerId,
        String subject,
        String vct
    ) {
        if (buisnessPartnerId == null) {
            buisnessPartnerId = BUSINESS_PARTNER_A_ID;
        }
        if (subject == null) {
            subject = SUBJECT_A;
        }
        if (vct == null) {
            vct = "vct:test";
        }
        return new ProtectedIssuanceAuthorizationV2RequestDto(
            buisnessPartnerId,
            subject,
            Instant.now(),
            Instant.now().plusSeconds(10),
            new ProtectedIssuanceAuthorizationV2RequestDto.ProtectedIssuanceAuthorizationDto(
                vct,
                Map.of("default", "test vctName lang: default"),
                Map.of("default", "test reason lang: default")
            )
        );
    }

    public static @Valid ProtectedVerificationAuthorizationV2RequestDto tsProtectedVerificationAuthorizationV2RequestDto(
        UUID buisnesspartnerId,
        String subject
    ) {
        if (buisnesspartnerId == null) {
            buisnesspartnerId = BUSINESS_PARTNER_A_ID;
        }
        if (subject == null) {
            subject = SUBJECT_A;
        }
        return new ProtectedVerificationAuthorizationV2RequestDto(
            buisnesspartnerId,
            subject,
            Instant.now(),
            Instant.now().plusSeconds(10),
            List.of(ProtectedVerificationAuthorizationV2RequestDto.AuthorizableFieldDto.AHV_NUMBER)
        );
    }

    static Map<String, String> localizedText(String defaultValue, Map<String, String> translations) {
        var result = new LinkedHashMap<String, String>();
        result.put("default", defaultValue);
        result.putAll(translations);
        return result;
    }
}
