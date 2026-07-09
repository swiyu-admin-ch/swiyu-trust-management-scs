package ch.admin.bj.swiyu.trust.management.modules.management.service;

import static ch.admin.bj.swiyu.trust.management.modules.management.domain.details.TrustStatementPartnerLinkType.TRUST_STATEMENT_IDENTITY_V2;
import static ch.admin.bj.swiyu.trust.management.test.TrustStatementTestData.issuanceV1;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.verify;
import static org.springframework.data.domain.Pageable.unpaged;

import ch.admin.bj.swiyu.trust.management.modules.common.audit.AuditPublisher;
import ch.admin.bj.swiyu.trust.management.modules.management.api.*;
import ch.admin.bj.swiyu.trust.management.modules.management.config.issuer.IssuerJwtConfig;
import ch.admin.bj.swiyu.trust.management.modules.management.config.issuer.IssuerJwtProperties;
import ch.admin.bj.swiyu.trust.management.modules.management.config.statements.DefaultStatementProperties;
import ch.admin.bj.swiyu.trust.management.modules.management.domain.JwtStatementDomainService;
import ch.admin.bj.swiyu.trust.management.modules.management.domain.TrustStatementPartnerLinkRepository;
import ch.admin.bj.swiyu.trust.management.modules.management.domain.TrustStatementPartnerLinkValidator;
import ch.admin.bj.swiyu.trust.management.modules.management.domain.corebusiness.IssuerTrustRootProperties;
import ch.admin.bj.swiyu.trust.management.modules.registry.domain.StatementRepository;
import ch.admin.bj.swiyu.trust.management.modules.registry.service.JsonJwtDeserializer;
import ch.admin.bj.swiyu.trust.management.modules.registry.service.TrustRegistryService;
import ch.admin.bj.swiyu.trust.management.test.*;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
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
        IssuerJwtConfig.class,
        JwtStatementDomainService.class,
        JsonJwtDeserializer.class,
        TrustStatementService.class,
        TrustRegistryService.class,
        TrustStatementPartnerLinkValidator.class,
        DataJpaTestConfiguration.class,
        StatusListServiceTestConfiguration.class,
        AsyncTestConfig.class,
        MockAuditPublisherTestConfiguration.class,
    }
)
@EnableConfigurationProperties(
    { IssuerJwtProperties.class, DefaultStatementProperties.class, IssuerTrustRootProperties.class }
)
@ActiveProfiles("test")
class TrustStatementServiceIT {

    @Autowired
    TrustStatementService trustStatementService;

    @Autowired
    TrustStatementPartnerLinkRepository trustStatementPartnerLinkRepository;

    @Autowired
    ObjectMapper objectMapper;

    @Autowired
    AsyncTestConfig asyncTestConfig;

    @Autowired
    AuditPublisher auditPublisher;

    @Autowired
    StatementRepository statementRepository;

    private static void verifyTrustStatementPublishedAudit(
        AuditPublisher auditPublisher,
        UUID partnerLinkId,
        String partnerId,
        String statementType,
        String expectedJwt
    ) {
        var jwtCaptor = ArgumentCaptor.forClass(String.class);
        verify(auditPublisher).publishTrustStatement(
            eq(partnerLinkId.toString()),
            eq(partnerId),
            eq(statementType),
            eq(0L),
            anyString(),
            jwtCaptor.capture()
        );
        assertThat(jwtCaptor.getValue()).isEqualTo(expectedJwt);
    }

    private static void verifyTrustStatementDeactivatedAudit(
        AuditPublisher auditPublisher,
        UUID partnerLinkId,
        String partnerId,
        String statementType,
        String expectedJwt
    ) {
        var jwtCaptor = ArgumentCaptor.forClass(String.class);
        verify(auditPublisher).deactivateTrustStatement(
            eq(partnerLinkId.toString()),
            eq(partnerId),
            eq(statementType),
            eq(0L),
            anyString(),
            jwtCaptor.capture()
        );
        assertThat(jwtCaptor.getValue()).isEqualTo(expectedJwt);
    }

    private JsonNode toJson(String jsonString) throws JsonProcessingException {
        return objectMapper.readTree(jsonString);
    }

    private JsonNode toJson(Map<String, Object> object) {
        return objectMapper.valueToTree(object);
    }

    @BeforeEach
    void setUp() {
        asyncTestConfig.waitForAsyncOperationsFinished();
        reset(auditPublisher);
        statementRepository.deleteAllInBatch();
        trustStatementPartnerLinkRepository.deleteAllInBatch();
    }

    @Test
    void getSubmissions() {
        // GIVEN
        trustStatementPartnerLinkRepository.save(issuanceV1("did:1"));
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
        assertThat(result.getTotalElements()).isEqualTo(3);
        assertThat(result.getContent().getFirst().id()).isEqualTo(statement3.getId());
    }

    @Test
    void createIdentityV2Submission_emitsPublishAudit() {
        var partnerLink = trustStatementService.issueAndPublishIdentityV2TrustStatement(
            RequestTestData.tsIdentityV2RequestDto()
        );
        var registryJwt = statementRepository.findAll().getFirst().getSerialized();

        verifyTrustStatementPublishedAudit(
            auditPublisher,
            partnerLink.getId(),
            RequestTestData.BUSINESS_PARTNER_A_ID.toString(),
            TRUST_STATEMENT_IDENTITY_V2.name(),
            registryJwt
        );
    }

    @Test
    void deactivateIdentityV2_emitsDeactivationAuditWithJwt() {
        var partnerLink = trustStatementService.issueAndPublishIdentityV2TrustStatement(
            RequestTestData.tsIdentityV2RequestDto()
        );
        var registryJwt = statementRepository.findAll().getFirst().getSerialized();
        reset(auditPublisher);

        trustStatementService.deactivateTrustStatement(
            partnerLink.getId(),
            new DeactivationRequestDto("deactivated in test")
        );

        verifyTrustStatementDeactivatedAudit(
            auditPublisher,
            partnerLink.getId(),
            RequestTestData.BUSINESS_PARTNER_A_ID.toString(),
            TRUST_STATEMENT_IDENTITY_V2.name(),
            registryJwt
        );
    }

    @Test
    void createIdentityV1Submission() throws JsonProcessingException {
        // GIVEN
        var request = RequestTestData.tsIdentityV1RequestDto();
        var partnerId = UUID.randomUUID();
        // WHEN
        var partnerLink = trustStatementService.issueAndPublishIdentityV1TrustStatement(partnerId, request);
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
        var partnerLink = trustStatementService.issueAndPublishIssuanceV1TrustStatement(partnerId, request);
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
        var partnerLink = trustStatementService.issueAndPublishVerificationV1TrustStatement(partnerId, request);
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
}
