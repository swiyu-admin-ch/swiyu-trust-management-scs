package ch.admin.bj.swiyu.trust.management.modules.management.service;

import static ch.admin.bj.swiyu.trust.management.test.TrustStatementTestData.issuanceV1;
import static ch.admin.bj.swiyu.trust.management.test.TrustStatementTestData.metadataV1;
import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.data.domain.Pageable.unpaged;

import ch.admin.bj.swiyu.trust.management.modules.management.api.TrustRegistryStatusDto;
import ch.admin.bj.swiyu.trust.management.modules.management.api.TrustStatementPartnerLinkFilterDto;
import ch.admin.bj.swiyu.trust.management.modules.management.api.TrustStatementTypeDto;
import ch.admin.bj.swiyu.trust.management.modules.management.domain.TrustStatementPartnerLinkRepository;
import ch.admin.bj.swiyu.trust.management.modules.management.domain.TrustStatementPartnerLinkValidator;
import ch.admin.bj.swiyu.trust.management.modules.registry.service.TrustRegistryService;
import ch.admin.bj.swiyu.trust.management.test.DataJpaTestConfiguration;
import ch.admin.bj.swiyu.trust.management.test.PostgreSQLContainerInitializer;
import ch.admin.bj.swiyu.trust.management.test.RequestTestData;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.context.annotation.Import;
import org.springframework.data.domain.Sort;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.ContextConfiguration;
import org.testcontainers.junit.jupiter.Testcontainers;

@Testcontainers
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@ContextConfiguration(initializers = PostgreSQLContainerInitializer.class)
@DataJpaTest
@Import(
    {
        TrustStatementService.class,
        TrustRegistryService.class,
        TrustStatementPartnerLinkValidator.class,
        DataJpaTestConfiguration.class,
    }
)
@ActiveProfiles("test")
class TrustStatementServiceIT {

    @Autowired
    TrustStatementService trustStatementService;

    @Autowired
    TrustStatementPartnerLinkRepository trustStatementPartnerLinkRepository;

    @Autowired
    ObjectMapper objectMapper;

    @BeforeEach
    void setUp() {
        trustStatementPartnerLinkRepository.deleteAllInBatch();
    }

    @Test
    void getSubmissions() {
        // GIVEN
        trustStatementPartnerLinkRepository.save(metadataV1("did:1"));
        trustStatementPartnerLinkRepository.save(issuanceV1("did:2"));
        var statement3 = trustStatementPartnerLinkRepository.saveAndFlush(issuanceV1("did:3"));
        // WHEN
        var result = trustStatementService.getPartnerLinks(
            new TrustStatementPartnerLinkFilterDto(
                null, // subject
                TrustStatementTypeDto.ISSUANCE_V1,
                null, // trustRegistryStatus
                null, // lastModifiedBy
                null // createdBy
            ),
            unpaged(Sort.by(new Sort.Order(Sort.Direction.DESC, "subject")))
        );
        // THEN
        assertThat(result.getTotalElements()).isEqualTo(2);
        assertThat(result.getContent().getFirst().id()).isEqualTo(statement3.getId());
    }

    @Test
    void createMetadataV1Submission() throws JsonProcessingException {
        // GIVEN
        var request = RequestTestData.tsMetadataV1RequestDto();
        // WHEN
        var partnerLink = trustStatementService.issueAndPublishMetadataTrustStatement(request);
        // THEN
        assertThat(partnerLink.getTrustRegistryStatus()).isEqualTo(TrustRegistryStatusDto.ACTIVE);
        assertThat(partnerLink.getType()).isEqualTo(TrustStatementTypeDto.METADATA_V1);
        assertThat(partnerLink.getDetails()).containsEntry(
            "preferredLanguage",
            request.getPreferredLanguage().toString()
        );
        assertThat(toJson(partnerLink.getDetails())).isEqualTo(
            toJson(
                """
                {
                    "type":"TRUST_STATEMENT_METADATA_V1",
                    "preferredLanguage":"de-CH",
                    "orgName":{
                        "de-CH":"Beispielorganisation",
                        "it-CH":"Organizzazione di esempio",
                        "fr-CH":"Exemple d'organisation",
                        "en":"Example Organization",
                        "rm-CH":"organisaziun exemplarica"
                    },
                    "logoUri":{
                        "de-CH":"data:image/png;base64,abc",
                        "en":"data:image/png;base64,abc"
                    }
                }
                """
            )
        );
    }

    @Test
    void createIdentityV1Submission() throws JsonProcessingException {
        // GIVEN
        var request = RequestTestData.tsIdentityV1RequestDto();
        var partnerId = UUID.randomUUID();
        // WHEN
        var partnerLink = trustStatementService.issueAndPublishIdentityTrustStatement(partnerId, request);
        // THEN
        assertThat(partnerLink.getTrustRegistryStatus()).isEqualTo(TrustRegistryStatusDto.ACTIVE);
        assertThat(partnerLink.getType()).isEqualTo(TrustStatementTypeDto.IDENTITY_V1);
        assertThat(toJson(partnerLink.getDetails())).isEqualTo(
            toJson(
                """
                {
                    "type":"TRUST_STATEMENT_IDENTITY_V1",
                    "entityName":{
                        "de-CH":"Beispielorganisation",
                        "it-CH":"Organizzazione di esempio",
                        "fr-CH":"Exemple d'organisation",
                        "en":"Example Organization",
                        "rm-CH":"organisaziun exemplarica"
                    },
                    "registryIds":[
                        {
                            "type":"UID",
                            "value":"CHE-000.000.000"
                        },
                        {
                            "type":"LEI",
                            "value":"0A1B2C3D4E5F6G7H8J9I"
                        }
                    ],
                    "isStateActor":true
                }
                """
            )
        );
    }

    @Test
    void createIssuanceV1Submission() throws JsonProcessingException {
        // GIVEN
        var request = RequestTestData.tsIssuanceV1RequestDto();
        var partnerId = UUID.randomUUID();
        // WHEN
        var partnerLink = trustStatementService.issueAndPublishIssuanceTrustStatement(partnerId, request);
        // THEN
        assertThat(partnerLink.getTrustRegistryStatus()).isEqualTo(TrustRegistryStatusDto.ACTIVE);
        assertThat(partnerLink.getType()).isEqualTo(TrustStatementTypeDto.ISSUANCE_V1);
        assertThat(toJson(partnerLink.getDetails())).isEqualTo(
            toJson(
                """
                        {
                          "type": "TRUST_STATEMENT_ISSUANCE_V1",
                          "canIssue": "https://trust-reg.trust-infra.swiyu.admin.ch/api/v1/vc-schema/test-vc-schema"
                        }
                """
            )
        );
    }

    @Test
    void createVerficationV1Submission() throws JsonProcessingException {
        // GIVEN
        var request = RequestTestData.tsVerificationV1RequestDto();
        var partnerId = UUID.randomUUID();
        // WHEN
        var partnerLink = trustStatementService.issueAndPublishVerificationTrustStatement(partnerId, request);
        // THEN
        assertThat(partnerLink.getTrustRegistryStatus()).isEqualTo(TrustRegistryStatusDto.ACTIVE);
        assertThat(partnerLink.getType()).isEqualTo(TrustStatementTypeDto.VERIFICATION_V1);
        assertThat(toJson(partnerLink.getDetails())).isEqualTo(
            toJson(
                """
                        {
                          "type": "TRUST_STATEMENT_VERIFICATION_V1",
                          "canVerify": "https://trust-reg.trust-infra.swiyu.admin.ch/api/v1/vc-schema/test-vc-schema"
                        }
                """
            )
        );
    }

    private JsonNode toJson(String jsonString) throws JsonProcessingException {
        return objectMapper.readTree(jsonString);
    }

    private JsonNode toJson(Map<String, Object> object) {
        return objectMapper.valueToTree(object);
    }
}
