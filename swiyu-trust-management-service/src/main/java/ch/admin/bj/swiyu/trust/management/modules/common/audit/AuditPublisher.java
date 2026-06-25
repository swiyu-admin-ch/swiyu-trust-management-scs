package ch.admin.bj.swiyu.trust.management.modules.common.audit;

import static ch.admin.bit.jeap.audit.record.create.AuditObjectDataRole.NEW;
import static ch.admin.bj.swiyu.trust.management.modules.common.audit.AuditEventDataKey.BUSINESS_PARTNER_ID;
import static ch.admin.bj.swiyu.trust.management.modules.common.audit.AuditEventDataKey.USE_CASE_CATEGORY_ID;
import static ch.admin.bj.swiyu.trust.management.modules.common.audit.AuditUseCase.NON_COMPLIANT_ACTOR_ADDED;
import static ch.admin.bj.swiyu.trust.management.modules.common.audit.AuditUseCase.NON_COMPLIANT_ACTOR_DELETED;
import static ch.admin.bj.swiyu.trust.management.modules.common.audit.AuditUseCase.TRUST_STATEMENT_DEACTIVATED;
import static ch.admin.bj.swiyu.trust.management.modules.common.audit.AuditUseCase.TRUST_STATEMENT_PUBLISHED;
import static ch.admin.bj.swiyu.trust.management.modules.common.audit.AuditUseCase.VC_SCHEMA_PUBLISHED;
import static ch.admin.bj.swiyu.trust.management.modules.common.audit.AuditorProvider.getCurrentAuditor;

import ch.admin.bit.jeap.audit.command.builder.CreateAuditRecordCommandBuilder;
import ch.admin.bit.jeap.audit.record.create.CreateAuditRecordCommand;
import ch.admin.bit.jeap.audit.transactional.outbox.CreateAuditRecordCommandTransactionOutboxSender;
import ch.admin.bit.jeap.messaging.annotations.JeapMessageProducerContract;
import ch.admin.bit.jeap.messaging.kafka.properties.KafkaProperties;
import io.micrometer.tracing.Tracer;
import java.time.Instant;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

@Component
@AllArgsConstructor
@Slf4j
@JeapMessageProducerContract(
    value = CreateAuditRecordCommand.TypeRef.class,
    topic = "ti-create-audit-record"
    // encryptionKeyId = "messagingKey" - Add encryption as soon as it is correctly configured
)
public class AuditPublisher {

    private static final String DEPARTMENT_NAME = "BJ";

    private final CreateAuditRecordCommandTransactionOutboxSender sender;
    private final KafkaProperties kafkaProperties;
    private final Tracer tracer;

    @Transactional(propagation = Propagation.MANDATORY)
    public void createNonCompliantActor(String nonCompliantActorId, Long version, String nonCompliantActorJson) {
        publishAuditEvent(NON_COMPLIANT_ACTOR_ADDED, nonCompliantActorId, version, nonCompliantActorJson);
    }

    @Transactional(propagation = Propagation.MANDATORY)
    public void deleteNonCompliantActor(String nonCompliantActorId, Long version, String nonCompliantActorJson) {
        publishAuditEvent(NON_COMPLIANT_ACTOR_DELETED, nonCompliantActorId, version, nonCompliantActorJson);
    }

    @Transactional(propagation = Propagation.MANDATORY)
    public void deactivateTrustStatement(
        String trustStatementId,
        String partnerId,
        String trustStatementType,
        Long version,
        String trustStatementEntryJson,
        String trustStatementJwt
    ) {
        publishAuditEvent(
            TRUST_STATEMENT_DEACTIVATED,
            trustStatementId,
            version,
            trustStatementType,
            partnerId,
            trustStatementEntryJson,
            trustStatementJwt
        );
    }

    @Transactional(propagation = Propagation.MANDATORY)
    public void publishTrustStatement(
        String trustStatementId,
        String partnerId,
        String trustStatementType,
        Long version,
        String trustStatementEntryJson,
        String trustStatementJwt
    ) {
        publishAuditEvent(
            TRUST_STATEMENT_PUBLISHED,
            trustStatementId,
            version,
            trustStatementType,
            partnerId,
            trustStatementEntryJson,
            trustStatementJwt
        );
    }

    @Transactional(propagation = Propagation.MANDATORY)
    public void processVcSchemaSubmissionAccepted(
        String vcSchemaId,
        String partnerId,
        Long version,
        String vcSchemaJson,
        String vcSchemaFile
    ) {
        publishAuditEvent(VC_SCHEMA_PUBLISHED, vcSchemaId, version, null, partnerId, vcSchemaJson, vcSchemaFile);
    }

    private void publishAuditEvent(AuditUseCase useCase, String objectId, Long version, String metaJson) {
        publishAuditEvent(useCase, objectId, version, null, null, metaJson, null);
    }

    private void publishAuditEvent(
        AuditUseCase useCase,
        String objectId,
        Long version,
        String auditObjectType,
        String businessPartnerId,
        String metaJson,
        String document
    ) {
        log.info("Sending audit event: useCase={}, objectId={}", useCase.getName(), objectId);

        var builder = withCommonFields(
            useCase,
            objectId,
            version,
            auditObjectType,
            businessPartnerId
        ).addAuditObjectDataJSON(NEW, useCase.getMetaFieldName(), metaJson);

        if (useCase.hasDataField() && document != null) {
            builder.addAuditObjectDataValue(NEW, useCase.getDataFieldName(), document);
        }

        sender.auditEvent(builder.build());
    }

    private CreateAuditRecordCommandBuilder withCommonFields(
        AuditUseCase useCase,
        String objectId,
        Long version,
        String auditObjectType,
        String businessPartnerId
    ) {
        var timestamp = Instant.now();
        var serviceName = kafkaProperties.getServiceName();
        var systemName = kafkaProperties.getSystemName();
        var builder = CreateAuditRecordCommandBuilder.createCommandBuilder(serviceName, systemName, timestamp);
        var auditor = getCurrentAuditor(SecurityContextHolder.getContext().getAuthentication());
        if (auditor.isSystem()) {
            builder.setTriggerSystem(DEPARTMENT_NAME, systemName, serviceName);
        } else {
            builder.setTriggerUser(auditor.auditUserId(), auditor.identityProvider());
        }

        builder
            .idempotenceId(objectId + "-" + useCase.getName() + "-" + timestamp)
            .setEventType(useCase.getEventType())
            .setContext(useCase.getName(), getCurrentTraceId())
            .setAuditObject(useCase.resolveAuditObjectType(auditObjectType), objectId, version.toString())
            .addEventData(USE_CASE_CATEGORY_ID.getKey(), useCase.getCategory());

        if (useCase.isIncludesBusinessPartnerId()) {
            builder.addEventData(BUSINESS_PARTNER_ID.getKey(), businessPartnerId);
        }

        return builder;
    }

    private String getCurrentTraceId() {
        var span = tracer.currentSpan();
        if (span == null) {
            log.error("No current span available, cannot get trace id for audit record");
            return null;
        }
        return span.context().traceId();
    }
}
