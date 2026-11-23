package ch.admin.bj.swiyu.trust.management.modules.management.service;

import static ch.admin.bj.swiyu.trust.management.modules.management.domain.TrustStatementPartnerLinkStatus.ACTIVE;
import static ch.admin.bj.swiyu.trust.management.modules.management.domain.TrustStatementPartnerLinkStatus.INACTIVE;
import static ch.admin.bj.swiyu.trust.management.modules.management.domain.details.TrustStatementPartnerLinkType.STATEMENT_TOMBSTONE;
import static ch.admin.bj.swiyu.trust.management.modules.management.domain.details.TrustStatementPartnerLinkType.TRUST_STATEMENT_IDENTITY_V2;
import static ch.admin.bj.swiyu.trust.management.test.TrustStatementTestData.issuanceV1;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.verify;
import static org.springframework.data.domain.Pageable.unpaged;

import ch.admin.bj.swiyu.trust.client.core.business.internal.api.IdentifierApi;
import ch.admin.bj.swiyu.trust.management.modules.common.audit.AuditPublisher;
import ch.admin.bj.swiyu.trust.management.modules.common.exception.ResourceNotFoundException;
import ch.admin.bj.swiyu.trust.management.modules.management.api.*;
import ch.admin.bj.swiyu.trust.management.modules.management.config.issuer.IssuerJwtConfig;
import ch.admin.bj.swiyu.trust.management.modules.management.config.issuer.IssuerJwtProperties;
import ch.admin.bj.swiyu.trust.management.modules.management.config.statements.DefaultStatementProperties;
import ch.admin.bj.swiyu.trust.management.modules.management.domain.BusinessPartnerIdentity;
import ch.admin.bj.swiyu.trust.management.modules.management.domain.BusinessPartnerIdentityRepository;
import ch.admin.bj.swiyu.trust.management.modules.management.domain.BusinessPartnerIdentityStatus;
import ch.admin.bj.swiyu.trust.management.modules.management.domain.JwtStatementDomainService;
import ch.admin.bj.swiyu.trust.management.modules.management.domain.StatusListMetadataRepository;
import ch.admin.bj.swiyu.trust.management.modules.management.domain.TokenStatusListBit;
import ch.admin.bj.swiyu.trust.management.modules.management.domain.TokenStatusListToken;
import ch.admin.bj.swiyu.trust.management.modules.management.domain.TrustStatementPartnerLink;
import ch.admin.bj.swiyu.trust.management.modules.management.domain.TrustStatementPartnerLinkRepository;
import ch.admin.bj.swiyu.trust.management.modules.management.domain.TrustStatementPartnerLinkValidator;
import ch.admin.bj.swiyu.trust.management.modules.management.domain.corebusiness.IssuerTrustRootProperties;
import ch.admin.bj.swiyu.trust.management.modules.registry.domain.DatastoreEntity;
import ch.admin.bj.swiyu.trust.management.modules.registry.domain.DatastoreEntityRepository;
import ch.admin.bj.swiyu.trust.management.modules.registry.domain.StatementRepository;
import ch.admin.bj.swiyu.trust.management.modules.registry.domain.VcEntity;
import ch.admin.bj.swiyu.trust.management.modules.registry.domain.VcEntityRepository;
import ch.admin.bj.swiyu.trust.management.modules.registry.service.JsonJwtDeserializer;
import ch.admin.bj.swiyu.trust.management.modules.registry.service.TrustRegistryService;
import ch.admin.bj.swiyu.trust.management.test.*;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase;
import org.springframework.context.annotation.Import;
import org.springframework.data.domain.Sort;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.testcontainers.junit.jupiter.Testcontainers;
import tools.jackson.core.JacksonException;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

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
        NonCompliantActorDidsResolver.class,
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

    @Autowired
    BusinessPartnerIdentityRepository businessPartnerIdentityRepository;

    @Autowired
    DatastoreEntityRepository datastoreEntityRepository;

    @Autowired
    VcEntityRepository vcEntityRepository;

    @Autowired
    StatusListMetadataRepository statusListMetadataRepository;

    @Autowired
    JwtStatementDomainService jwtStatementDomainService;

    @Autowired
    TrustRegistryService trustRegistryService;

    @MockitoBean
    IdentifierApi identifierApi;

    private static void verifyTrustStatementPublishedAudit(
        AuditPublisher auditPublisher,
        UUID partnerLinkId,
        String partnerId,
        String statementType,
        String expectedJwt
    ) {
        var jwtCaptor = ArgumentCaptor.forClass(String.class);
        verify(auditPublisher).trustStatementPublished(
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

    private JsonNode toJson(String jsonString) throws JacksonException {
        return objectMapper.readTree(jsonString);
    }

    private JsonNode toJson(Map<String, Object> object) {
        return objectMapper.valueToTree(object);
    }

    @BeforeEach
    void setUp() {
        asyncTestConfig.waitForAsyncOperationsFinished();
        reset(auditPublisher);
        vcEntityRepository.deleteAllInBatch();
        datastoreEntityRepository.deleteAllInBatch();
        statementRepository.deleteAllInBatch();
        trustStatementPartnerLinkRepository.deleteAllInBatch();
        businessPartnerIdentityRepository.deleteAllInBatch();
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
                null,
                null,
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
    void getPartnerLinks_hidesStatementTombstones() {
        // GIVEN
        trustStatementPartnerLinkRepository.saveAndFlush(issuanceV1("did:visible"));
        var tombstone = trustStatementPartnerLinkRepository.saveAndFlush(issuanceV1("did:tombstone"));
        tombstone.convertToTombstone();
        trustStatementPartnerLinkRepository.saveAndFlush(tombstone);

        // WHEN
        var result = trustStatementService.getPartnerLinks(
            new TrustStatementPartnerLinkFilterDto(null, null, null, null, null, null, null),
            unpaged()
        );

        // THEN
        assertThat(result.getTotalElements()).isEqualTo(1);
        assertThat(result.getContent())
            .extracting(TrustStatementPartnerLinkListItemDto::subject)
            .containsExactly("did:visible");
        assertThat(trustStatementService.count()).isEqualTo(1);
        assertThatThrownBy(() -> trustStatementService.getPartnerLink(tombstone.getId())).isInstanceOf(
            ResourceNotFoundException.class
        );
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
    void hardDeleteByPartnerId_tombstonesThePartnersStatementsInBothDatabases() throws Exception {
        // GIVEN a partner identity and four statements: published V2, legacy V2 without partner id, unpublished V2,
        // published V1
        var partnerId = UUID.randomUUID();
        var did = "did:example:hard-deleted";
        businessPartnerIdentityRepository.saveAndFlush(businessPartnerIdentity(partnerId, did));

        var publishedV2 = trustStatementService.issueAndPublishIdentityV2TrustStatement(
            RequestTestData.tsIdentityV2RequestDto(partnerId, did)
        );
        var publishedV2Entity = trustStatementPartnerLinkRepository.findById(publishedV2.getId()).orElseThrow();
        trustStatementPartnerLinkRepository.saveAndFlush(
            TrustStatementPartnerLink.createIdentityV2(
                null, // created before partner_id was introduced
                did,
                Instant.now(),
                Instant.now().plusSeconds(3600),
                Map.of("default", "Legacy"),
                List.of(),
                false,
                null
            )
        );
        var notPublished = trustStatementPartnerLinkRepository.saveAndFlush(
            TrustStatementPartnerLink.createIdentityV2(
                partnerId,
                "did:example:not-published",
                Instant.now(),
                Instant.now().plusSeconds(3600),
                Map.of("default", "Draft"),
                List.of(),
                false,
                null
            )
        );
        var datastore = datastoreEntityRepository.saveAndFlush(new DatastoreEntity());
        vcEntityRepository.saveAndFlush(new VcEntity(datastore.getId(), "raw-vc", objectMapper.valueToTree(Map.of())));
        var publishedV1 = TrustStatementPartnerLink.createIssuanceV1(
            partnerId,
            did,
            Instant.now(),
            Instant.now().plusSeconds(3600),
            "https://example.com/vct"
        );
        publishedV1.persistReferencesAfterPublicationSucceeded(UUID.randomUUID(), datastore.getId(), ACTIVE);
        trustStatementPartnerLinkRepository.saveAndFlush(publishedV1);

        // WHEN
        trustStatementService.hardDeleteByPartnerId(partnerId);

        // THEN every statement is a content-free tombstone
        assertThat(trustStatementPartnerLinkRepository.findAll())
            .hasSize(4)
            .allSatisfy(link -> {
                assertThat(link.getType()).isEqualTo(STATEMENT_TOMBSTONE);
                assertThat(link.getStatus()).isEqualTo(INACTIVE);
                assertThat(link.getSubject()).isNull();
                assertThat(link.getDetails()).isNull();
            });
        assertThat(
            trustStatementPartnerLinkRepository.findById(publishedV2Entity.getId()).orElseThrow().getStatusListIndex()
        ).isEqualTo(publishedV2Entity.getStatusListIndex());
        // AND the public copies in the registry database are gone
        assertThat(statementRepository.findById(publishedV2.getId())).isEmpty();
        assertThat(datastoreEntityRepository.findById(datastore.getId())).isEmpty();
        assertThat(vcEntityRepository.findByDatastoreEntityId(datastore.getId())).isEmpty();
        // AND the status list still revokes the index of the tombstoned statement
        var statusList = statusListMetadataRepository
            .findById(publishedV2Entity.getStatusListMetadataId())
            .orElseThrow();
        var revoked = trustStatementPartnerLinkRepository.findAllByStatusListMetadataIdAndStatus(
            statusList.getId(),
            INACTIVE
        );
        var claims = jwtStatementDomainService
            .generateTokenStatusList(statusList, revoked)
            .getJWTClaimsSet()
            .getJSONObjectClaim("status_list");
        var token = TokenStatusListToken.loadTokenStatusListToken(
            ((Number) claims.get("bits")).intValue(),
            (String) claims.get("lst")
        );
        assertThat(token.getStatus(publishedV2Entity.getStatusListIndex())).isEqualTo(
            TokenStatusListBit.REVOKE.getValue()
        );
        // AND the never-published statement was audited before its content was removed
        verify(auditPublisher).deactivateTrustStatement(
            eq(notPublished.getId().toString()),
            eq(partnerId.toString()),
            eq(TRUST_STATEMENT_IDENTITY_V2.name()),
            eq(0L),
            anyString(),
            eq("")
        );
        // AND the published statement was deactivated before it was tombstoned
        verify(auditPublisher).deactivateTrustStatement(
            eq(publishedV2.getId().toString()),
            eq(partnerId.toString()),
            eq(TRUST_STATEMENT_IDENTITY_V2.name()),
            eq(0L),
            anyString(),
            anyString()
        );

        // AND a second run changes nothing
        trustStatementService.hardDeleteByPartnerId(partnerId);
        assertThat(trustStatementPartnerLinkRepository.findAll()).hasSize(4);
    }

    private static BusinessPartnerIdentity businessPartnerIdentity(UUID partnerId, String did) {
        return new BusinessPartnerIdentity(
            partnerId,
            Map.of("default", "Acme AG"),
            Instant.now(),
            "CHE-123.456.789",
            true,
            "de",
            BusinessPartnerIdentityStatus.ACTIVE,
            false,
            Instant.now().plusSeconds(3600),
            null,
            Set.of(did)
        );
    }

    @Test
    void hardDeleteByPartnerId_convergesWhenTheRegistryCopyIsAlreadyGone() {
        var partnerId = UUID.randomUUID();
        var did = "did:example:partial-run";
        businessPartnerIdentityRepository.saveAndFlush(businessPartnerIdentity(partnerId, did));
        var published = trustStatementService.issueAndPublishIdentityV2TrustStatement(
            RequestTestData.tsIdentityV2RequestDto(partnerId, did)
        );
        // A previous run deleted the public copy but did not commit the tombstone.
        trustRegistryService.hardDeleteStatement(published.getId());

        trustStatementService.hardDeleteByPartnerId(partnerId);

        var reloaded = trustStatementPartnerLinkRepository.findById(published.getId()).orElseThrow();
        assertThat(reloaded.getType()).isEqualTo(STATEMENT_TOMBSTONE);
        assertThat(reloaded.getStatus()).isEqualTo(INACTIVE);
        verify(auditPublisher).deactivateTrustStatement(
            eq(published.getId().toString()),
            eq(partnerId.toString()),
            eq(TRUST_STATEMENT_IDENTITY_V2.name()),
            eq(0L),
            anyString(),
            anyString()
        );
    }

    @Test
    void createIdentityV1Submission() throws JacksonException {
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
    void createIssuanceV1Submission() throws JacksonException {
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
    void createVerficationV1Submission() throws JacksonException {
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
