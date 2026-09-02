package ch.admin.bj.swiyu.trust.management.modules.common.audit;

import static ch.admin.bit.jeap.audit.record.create.AuditObjectDataRole.NEW;
import static ch.admin.bj.swiyu.trust.management.modules.common.audit.AuditEventDataKey.BUSINESS_PARTNER_ID;
import static ch.admin.bj.swiyu.trust.management.modules.common.audit.AuditEventDataKey.USE_CASE_CATEGORY_ID;
import static ch.admin.bj.swiyu.trust.management.modules.common.audit.AuditUseCase.*;
import static ch.admin.bj.swiyu.trust.management.modules.common.audit.AuditorProvider.getCurrentAuditor;

import ch.admin.bit.jeap.audit.command.builder.CreateAuditRecordCommandBuilder;
import ch.admin.bit.jeap.audit.record.create.CreateAuditRecordCommand;
import ch.admin.bit.jeap.audit.transactional.outbox.CreateAuditRecordCommandTransactionOutboxSender;
import ch.admin.bit.jeap.messaging.annotations.JeapMessageProducerContract;
import ch.admin.bit.jeap.messaging.kafka.properties.KafkaProperties;
import io.micrometer.tracing.Tracer;
import java.time.Instant;
import java.util.UUID;
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
    public void nonCompliantActorAdded(String nonCompliantActorId, Long version, String nonCompliantActorJson) {
        publishAuditEvent(NON_COMPLIANT_ACTOR_ADDED, nonCompliantActorId, version, nonCompliantActorJson);
    }

    @Transactional(propagation = Propagation.MANDATORY)
    public void nonCompliantActorDeleted(String nonCompliantActorId, Long version, String nonCompliantActorJson) {
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
    public void trustStatementPublished(
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
    public void taskApproved(UUID partnerId, UUID taskId, Long version, String taskType, String taskJson) {
        publishAuditEvent(
            TASK_APPROVED,
            taskId.toString(),
            version,
            taskType,
            partnerId.toString(),
            taskJson,
            null // no non-json data part of auditing
        );
    }

    @Transactional(propagation = Propagation.MANDATORY)
    public void taskRejected(UUID partnerId, UUID taskId, Long version, String taskType, String taskJson) {
        publishAuditEvent(
            TASK_REJECTED,
            taskId.toString(),
            version,
            taskType,
            partnerId.toString(),
            taskJson,
            null // no non-json data part of auditing
        );
    }

    @Transactional(propagation = Propagation.MANDATORY)
    public void taskMoreInformationRequested(
        UUID partnerId,
        UUID taskId,
        Long version,
        String taskType,
        String taskJson
    ) {
        publishAuditEvent(
            TASK_MORE_INFORMATION_REQUESTED,
            taskId.toString(),
            version,
            taskType,
            partnerId.toString(),
            taskJson,
            null // no non-json data part of auditing
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

    private void publishAuditEvent(AuditUseCase useCase, String objectId, Long version, String dataJson) {
        publishAuditEvent(useCase, objectId, version, null, null, dataJson, null);
    }

    private void publishAuditEvent(
        AuditUseCase useCase,
        String objectId,
        Long version,
        String auditObjectType,
        String businessPartnerId,
        String dataJson,
        String dataValue
    ) {
        log.info("Sending audit event: useCase={}, objectId={}", useCase.name(), objectId);

        var builder = withCommonFields(
            useCase,
            objectId,
            version,
            auditObjectType,
            businessPartnerId
        ).addAuditObjectDataJSON(NEW, useCase.getDataJsonFieldName(), dataJson);

        if (useCase.hasDataField() && dataValue != null) {
            builder.addAuditObjectDataValue(NEW, useCase.getDataValueFieldName(), dataValue);
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
            .idempotenceId(objectId + "-" + useCase.name() + "-" + timestamp)
            .setEventType(useCase.getEventType())
            .setContext(useCase.name(), getCurrentTraceId())
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
