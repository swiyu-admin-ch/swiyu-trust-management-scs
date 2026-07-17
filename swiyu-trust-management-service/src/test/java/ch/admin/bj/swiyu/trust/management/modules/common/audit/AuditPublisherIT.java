package ch.admin.bj.swiyu.trust.management.modules.common.audit;

import static ch.admin.bj.swiyu.trust.management.modules.common.audit.AuditEventDataKey.USE_CASE_CATEGORY_ID;
import static ch.admin.bj.swiyu.trust.management.modules.common.audit.AuditTestSupport.assertAuditedObject;
import static ch.admin.bj.swiyu.trust.management.modules.common.audit.AuditTestSupport.assertBusinessPartnerId;
import static ch.admin.bj.swiyu.trust.management.modules.common.audit.AuditTestSupport.assertJsonObjectData;
import static ch.admin.bj.swiyu.trust.management.modules.common.audit.AuditTestSupport.assertValueObjectData;
import static ch.admin.bj.swiyu.trust.management.modules.common.audit.AuditTestSupport.clearDeferredMessages;
import static ch.admin.bj.swiyu.trust.management.modules.common.audit.AuditUseCase.NON_COMPLIANT_ACTOR_ADDED;
import static ch.admin.bj.swiyu.trust.management.modules.common.audit.AuditUseCase.NON_COMPLIANT_ACTOR_DELETED;
import static ch.admin.bj.swiyu.trust.management.modules.common.audit.AuditUseCase.TRUST_STATEMENT_DEACTIVATED;
import static ch.admin.bj.swiyu.trust.management.modules.common.audit.AuditUseCase.TRUST_STATEMENT_PUBLISHED;
import static ch.admin.bj.swiyu.trust.management.modules.common.audit.AuditUseCase.VC_SCHEMA_PUBLISHED;
import static ch.admin.bj.swiyu.trust.management.modules.common.auth.UserRole.Names.EDITOR;
import static ch.admin.bj.swiyu.trust.management.modules.management.domain.details.TrustStatementPartnerLinkType.TRUST_STATEMENT_IDENTITY_V2;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import ch.admin.bit.jeap.audit.record.create.AuditObjectDataJSON;
import ch.admin.bit.jeap.audit.record.create.AuditObjectDataRole;
import ch.admin.bit.jeap.audit.record.create.AuditUser;
import ch.admin.bit.jeap.audit.record.create.CreateAuditRecordCommand;
import ch.admin.bit.jeap.messaging.kafka.interceptor.JeapKafkaMessageCallback;
import ch.admin.bit.jeap.messaging.transactionaloutbox.outbox.DeferredMessageRepository;
import ch.admin.bit.jeap.security.test.resource.JeapAuthenticationTestTokenBuilder;
import ch.admin.bj.swiyu.trust.management.modules.management.domain.NonCompliantActor;
import ch.admin.bj.swiyu.trust.management.modules.management.domain.NonCompliantReasonText;
import ch.admin.bj.swiyu.trust.management.modules.management.domain.TrustStatementPartnerLink;
import ch.admin.bj.swiyu.trust.management.modules.management.domain.TrustStatementPartnerLinkStatus;
import ch.admin.bj.swiyu.trust.management.test.PostgreSQLContainerInitializer;
import io.micrometer.tracing.Tracer;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.kafka.test.context.EmbeddedKafka;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.transaction.annotation.Transactional;
import org.testcontainers.junit.jupiter.Testcontainers;

@ActiveProfiles("test")
@SpringBootTest
@EmbeddedKafka
@Testcontainers
@ContextConfiguration(initializers = PostgreSQLContainerInitializer.class)
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
class AuditPublisherIT {

    private static final String EIAM_REF_ISSUER = "https://identity-eiam-r.eiam.admin.ch/realms/ejpd_bj-swiyu-tms";
    private static final String EIAM_REF_AUDIT_USER_ID = "47443696:Mustermann,Max";
    private static final String PUBLISHER_SYSTEM = "swiyu-trust-business";
    private static final String PUBLISHER_SERVICE = "swiyu-trust-management-scs";

    private static final String NON_COMPLIANT_ACTOR_ID = "00000000-0000-0000-0000-000000000001";
    private static final Long NON_COMPLIANT_ACTOR_VERSION = 1L;
    private static final String TRUST_STATEMENT_ID = "00000000-0000-0000-0000-000000000010";
    private static final String PARTNER_ID = "9f425029-9775-4984-99ba-bacc60069502";
    private static final String TRUST_STATEMENT_JWT = "eyJhbGciOiJub25lIn0.eyJzdWIiOiJ0ZXN0In0.";
    private static final String VC_SCHEMA_ID = "00000000-0000-0000-0000-000000000020";
    private static final Long VC_SCHEMA_VERSION = 2L;
    private static final String VC_SCHEMA_FILE =
        "{\"vct\":\"https://trust-reg.trust-infra.swiyu.admin.ch/api/v1/vc-schema/mySchema\"}";

    @Autowired
    AuditPublisher auditPublisher;

    @Autowired
    DeferredMessageRepository deferredMessageRepository;

    @MockitoBean
    JeapKafkaMessageCallback kafkaMsgCallback;

    @Autowired
    Tracer tracer;

    @BeforeEach
    @Transactional
    void setUp() {
        clearDeferredMessages(deferredMessageRepository);
        setEiamRefGovernmentUserInSecurityContext();
    }

    @Transactional
    @Test
    void createNonCompliantActor() {
        var nonCompliantActorJson = sampleNonCompliantActorJson();

        var traceId = executeWithTracing(() ->
            auditPublisher.createNonCompliantActor(
                NON_COMPLIANT_ACTOR_ID,
                NON_COMPLIANT_ACTOR_VERSION,
                nonCompliantActorJson
            )
        );

        var msg = verifySingleAuditCommand(deferredMessageRepository, kafkaMsgCallback);
        assertAuditEvent(msg, NON_COMPLIANT_ACTOR_ADDED, traceId);
        assertUseCaseCategory(msg, NON_COMPLIANT_ACTOR_ADDED);
        assertUserTrigger(msg);
        assertAuditedObject(
            msg,
            NON_COMPLIANT_ACTOR_ID,
            NON_COMPLIANT_ACTOR_ADDED.getAuditObjectType(),
            NON_COMPLIANT_ACTOR_VERSION.toString()
        );
        assertNonCompliantActorAuditPayload(msg, NON_COMPLIANT_ACTOR_ADDED, nonCompliantActorJson);
    }

    @Transactional
    @Test
    void deleteNonCompliantActor() {
        var nonCompliantActorJson = sampleNonCompliantActorJson();

        var traceId = executeWithTracing(() ->
            auditPublisher.deleteNonCompliantActor(
                NON_COMPLIANT_ACTOR_ID,
                NON_COMPLIANT_ACTOR_VERSION,
                nonCompliantActorJson
            )
        );

        var msg = verifySingleAuditCommand(deferredMessageRepository, kafkaMsgCallback);
        assertAuditEvent(msg, NON_COMPLIANT_ACTOR_DELETED, traceId);
        assertUseCaseCategory(msg, NON_COMPLIANT_ACTOR_DELETED);
        assertUserTrigger(msg);
        assertAuditedObject(
            msg,
            NON_COMPLIANT_ACTOR_ID,
            NON_COMPLIANT_ACTOR_DELETED.getAuditObjectType(),
            NON_COMPLIANT_ACTOR_VERSION.toString()
        );
        assertNonCompliantActorAuditPayload(msg, NON_COMPLIANT_ACTOR_DELETED, nonCompliantActorJson);
    }

    @Transactional
    @Test
    void publishTrustStatement() {
        var partnerLinkJson = sampleTrustStatementPartnerLinkJson();

        var traceId = executeWithTracing(() ->
            auditPublisher.publishTrustStatement(
                TRUST_STATEMENT_ID,
                PARTNER_ID,
                TRUST_STATEMENT_IDENTITY_V2.name(),
                0L,
                partnerLinkJson,
                TRUST_STATEMENT_JWT
            )
        );

        var msg = verifySingleAuditCommand(deferredMessageRepository, kafkaMsgCallback);
        assertAuditEvent(msg, TRUST_STATEMENT_PUBLISHED, traceId);
        assertAuditedDataIdAndType(msg, TRUST_STATEMENT_ID, TRUST_STATEMENT_IDENTITY_V2.name());
        assertTrustStatementAuditPayload(
            msg,
            TRUST_STATEMENT_PUBLISHED,
            PARTNER_ID,
            partnerLinkJson,
            TRUST_STATEMENT_JWT
        );
    }

    @Transactional
    @Test
    void deactivateTrustStatement() {
        var partnerLinkJson = sampleTrustStatementPartnerLinkJson();

        var traceId = executeWithTracing(() ->
            auditPublisher.deactivateTrustStatement(
                TRUST_STATEMENT_ID,
                PARTNER_ID,
                TRUST_STATEMENT_IDENTITY_V2.name(),
                0L,
                partnerLinkJson,
                TRUST_STATEMENT_JWT
            )
        );

        var msg = verifySingleAuditCommand(deferredMessageRepository, kafkaMsgCallback);
        assertAuditEvent(msg, TRUST_STATEMENT_DEACTIVATED, traceId);
        assertTrustStatementAuditPayload(
            msg,
            TRUST_STATEMENT_DEACTIVATED,
            PARTNER_ID,
            partnerLinkJson,
            TRUST_STATEMENT_JWT
        );
    }

    @Transactional
    @Test
    void processVcSchemaSubmissionAccepted() {
        var vcSchemaJson = "{\"id\":\"" + VC_SCHEMA_ID + "\"}";

        var traceId = executeWithTracing(() ->
            auditPublisher.processVcSchemaSubmissionAccepted(
                VC_SCHEMA_ID,
                PARTNER_ID,
                VC_SCHEMA_VERSION,
                vcSchemaJson,
                VC_SCHEMA_FILE
            )
        );

        var msg = verifySingleAuditCommand(deferredMessageRepository, kafkaMsgCallback);
        assertAuditEvent(msg, VC_SCHEMA_PUBLISHED, traceId);
        assertBusinessPartnerId(msg, PARTNER_ID);
        assertAuditedObject(msg, VC_SCHEMA_ID, VC_SCHEMA_PUBLISHED.getAuditObjectType(), VC_SCHEMA_VERSION.toString());
        assertJsonObjectData(msg, VC_SCHEMA_PUBLISHED.getMetaFieldName(), vcSchemaJson);
        assertValueObjectData(msg, VC_SCHEMA_PUBLISHED.getDataFieldName(), VC_SCHEMA_FILE);
    }

    private static void setEiamRefGovernmentUserInSecurityContext() {
        var context = SecurityContextHolder.createEmptyContext();
        context.setAuthentication(
            JeapAuthenticationTestTokenBuilder.create()
                .withSubject("47443696")
                .withGivenName("Max")
                .withFamilyName("Mustermann")
                .withClaim("iss", EIAM_REF_ISSUER)
                .withUserRoles(EDITOR)
                .build()
        );
        SecurityContextHolder.setContext(context);
    }

    private static CreateAuditRecordCommand verifySingleAuditCommand(
        DeferredMessageRepository repository,
        JeapKafkaMessageCallback kafkaMsgCallback
    ) {
        assertThat(repository.findAll()).hasSize(1);
        var captor = ArgumentCaptor.forClass(CreateAuditRecordCommand.class);
        verify(kafkaMsgCallback, times(1)).onSend(captor.capture(), any());
        var command = captor.getValue();
        assertThat(command.getPublisher().getSystem()).isEqualTo(PUBLISHER_SYSTEM);
        assertThat(command.getPublisher().getService()).isEqualTo(PUBLISHER_SERVICE);
        return command;
    }

    private static void assertAuditEvent(CreateAuditRecordCommand message, AuditUseCase useCase, String traceId) {
        assertThat(message.getPayload().getEvent().getType()).isEqualTo(useCase.getEventType());
        assertThat(message.getPayload().getEvent().getContext().getUseCase()).isEqualTo(useCase.getName());
        assertThat(message.getPayload().getEvent().getContext().getProcessId()).isEqualTo(traceId);
    }

    private static void assertUseCaseCategory(CreateAuditRecordCommand message, AuditUseCase useCase) {
        assertThat(message.getPayload().getEvent().getEventData()).anyMatch(
            d -> USE_CASE_CATEGORY_ID.getKey().equals(d.getKey()) && useCase.getCategory().equals(d.getValue())
        );
    }

    private static void assertUserTrigger(CreateAuditRecordCommand message) {
        assertThat(((AuditUser) message.getPayload().getTrigger()).getId()).isEqualTo(EIAM_REF_AUDIT_USER_ID);
        assertThat(((AuditUser) message.getPayload().getTrigger()).getIdentityProvider()).isEqualTo(EIAM_REF_ISSUER);
    }

    private static void assertAuditedDataIdAndType(
        CreateAuditRecordCommand message,
        String objectId,
        String objectType
    ) {
        assertThat(message.getPayload().getAuditedData().getId()).isEqualTo(objectId);
        assertThat(message.getPayload().getAuditedData().getType()).isEqualTo(objectType);
    }

    private static void assertNonCompliantActorAuditPayload(
        CreateAuditRecordCommand message,
        AuditUseCase useCase,
        String expectedJson
    ) {
        assertThat(getAuditObjectDataJSON(message, useCase.getMetaFieldName()).getRole()).isEqualTo(
            AuditObjectDataRole.NEW
        );
        assertJsonObjectData(message, useCase.getMetaFieldName(), expectedJson);
    }

    private static AuditObjectDataJSON getAuditObjectDataJSON(CreateAuditRecordCommand message, String name) {
        return message
            .getPayload()
            .getAuditedData()
            .getObjectData()
            .stream()
            .filter(AuditObjectDataJSON.class::isInstance)
            .map(AuditObjectDataJSON.class::cast)
            .filter(o -> o.getName().equals(name))
            .findFirst()
            .orElseThrow();
    }

    private static void assertTrustStatementAuditPayload(
        CreateAuditRecordCommand message,
        AuditUseCase useCase,
        String partnerId,
        String metaJson,
        String jwt
    ) {
        assertBusinessPartnerId(message, partnerId);
        assertJsonObjectData(message, useCase.getMetaFieldName(), metaJson);
        assertValueObjectData(message, useCase.getDataFieldName(), jwt);
    }

    private static String sampleNonCompliantActorJson() {
        var actor = new NonCompliantActor(
            UUID.fromString(NON_COMPLIANT_ACTOR_ID),
            "did:tdw:alpha123",
            new NonCompliantReasonText(null, null, null, "Violation of policy", null)
        );
        return AuditMapper.toAuditJson(actor);
    }

    private static String sampleTrustStatementPartnerLinkJson() {
        var partnerLink = TrustStatementPartnerLink.createIdentityV2(
            UUID.fromString(PARTNER_ID),
            "did:example:subject",
            Instant.parse("2025-01-01T00:00:00Z"),
            Instant.parse("2026-01-01T00:00:00Z"),
            Map.of(),
            List.of(),
            false,
            null
        );
        partnerLink.persistReferencesAfterPublicationSucceeded(
            UUID.fromString("00000000-0000-0000-0000-000000000099"),
            UUID.fromString(TRUST_STATEMENT_ID),
            TrustStatementPartnerLinkStatus.ACTIVE
        );
        return AuditMapper.toAuditJson(partnerLink);
    }

    private String executeWithTracing(Runnable r) {
        var span = tracer.nextSpan().start();
        try (var _ = tracer.withSpan(span)) {
            r.run();
        }
        return span.context().traceId();
    }
}
