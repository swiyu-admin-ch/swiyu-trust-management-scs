package ch.admin.bj.swiyu.trust.management.modules.management.domain.issuer;

import static ch.admin.bj.swiyu.trust.management.test.TrustStatementTestData.*;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import ch.admin.bj.swiyu.trust.management.test.DataJpaTestConfiguration;
import ch.admin.bj.swiyu.trust.management.test.PostgreSQLContainerInitializer;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.ContextConfiguration;
import org.testcontainers.junit.jupiter.Testcontainers;

@Testcontainers
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@ContextConfiguration(initializers = PostgreSQLContainerInitializer.class)
@DataJpaTest
@Import({ DataJpaTestConfiguration.class })
@ActiveProfiles("test")
class IssuerCredentialRequestFactoryIT {

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    void createIssuerCredentialRequest_IdentityV1() throws JsonProcessingException {
        // Given
        var partnerLink = identityV1("subject123");

        // When
        var request = IssuerCredentialRequestFactory.createIssuerCredentialRequest(partnerLink, List.of("statusList1"));

        // Then
        assertNotNull(request);
        assertThat(toJson(request)).isEqualTo(
            toJson(
                """
                {
                    "metadata_credential_supported_id":["ts_identity1_sd_jwt"],
                    "credential_subject_data":{
                        "sub":"subject123",
                        "registryIds": [{"type":"registryType1","value":"registryValue1"},{"type":"registryType2","value":"registryValue2"}],
                        "isStateActor":true,
                        "vct":"TrustStatementIdentityV1",
                        "entityName":{"rm-CH":"Numn da l'entità RM","fr-CH":"Nom de l'entité FR","de-CH":"Entity Name DE","it-CH":"Nome dell'entità IT","en":"Entity Name EN"}
                    },
                    "credential_metadata":{},
                    "offer_validity_seconds":null,
                    "credential_valid_until":"2026-01-01T00:00:00Z",
                    "credential_valid_from":"2025-01-01T00:00:00Z",
                    "status_lists":["statusList1"]
                }
                """
            )
        );
    }

    @Test
    void createIssuerCredentialRequest_IssuanceV1() throws JsonProcessingException {
        // Given
        var partnerLink = issuanceV1("subject123");

        // When
        var request = IssuerCredentialRequestFactory.createIssuerCredentialRequest(partnerLink, List.of("statusList1"));

        // Then
        assertNotNull(request);
        assertThat(toJson(request)).isEqualTo(
            toJson(
                """
                {
                    "metadata_credential_supported_id":["ts_issuance1_sd_jwt"],
                    "credential_subject_data":{
                        "sub":"subject123",
                        "vct":"TrustStatementIssuanceV1",
                        "canIssue":"https://trust-reg.trust-infra.swiyu.admin.ch/api/v1/vc-schema/test-vc-schema"
                    },
                    "credential_metadata":{},
                    "offer_validity_seconds":null,
                    "credential_valid_until":"2026-01-01T00:00:00Z",
                    "credential_valid_from":"2025-01-01T00:00:00Z",
                    "status_lists":["statusList1"]
                }
                """
            )
        );
    }

    @Test
    void createIssuerCredentialRequest_VerificationV1() throws JsonProcessingException {
        // Given
        var partnerLink = verificationV1("subject123");

        // When
        var request = IssuerCredentialRequestFactory.createIssuerCredentialRequest(partnerLink, List.of("statusList1"));

        // Then
        assertNotNull(request);
        assertThat(toJson(request)).isEqualTo(
            toJson(
                """
                {
                    "metadata_credential_supported_id":["ts_verification1_sd_jwt"],
                    "credential_subject_data":{
                        "sub":"subject123",
                        "vct":"TrustStatementVerificationV1",
                        "canVerify":"https://trust-reg.trust-infra.swiyu.admin.ch/api/v1/vc-schema/test-vc-schema"
                    },
                    "credential_metadata":{},
                    "offer_validity_seconds":null,
                    "credential_valid_until":"2026-01-01T00:00:00Z",
                    "credential_valid_from":"2025-01-01T00:00:00Z",
                    "status_lists":["statusList1"]
                }
                """
            )
        );
    }

    private JsonNode toJson(Object object) throws JsonProcessingException {
        return toJson(objectMapper.writeValueAsString(object));
    }

    private JsonNode toJson(String jsonString) throws JsonProcessingException {
        return objectMapper.readTree(jsonString);
    }
}
