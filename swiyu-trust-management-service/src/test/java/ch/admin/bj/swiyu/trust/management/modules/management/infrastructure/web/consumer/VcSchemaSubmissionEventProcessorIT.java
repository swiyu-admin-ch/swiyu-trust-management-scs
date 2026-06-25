package ch.admin.bj.swiyu.trust.management.modules.management.infrastructure.web.consumer;

import static ch.admin.bj.swiyu.trust.management.modules.common.audit.AuditTestSupport.assertAuditedObject;
import static ch.admin.bj.swiyu.trust.management.modules.common.audit.AuditTestSupport.assertBusinessPartnerId;
import static ch.admin.bj.swiyu.trust.management.modules.common.audit.AuditTestSupport.assertJsonObjectData;
import static ch.admin.bj.swiyu.trust.management.modules.common.audit.AuditTestSupport.assertValueObjectData;
import static ch.admin.bj.swiyu.trust.management.modules.common.audit.AuditTestSupport.clearDeferredMessages;
import static ch.admin.bj.swiyu.trust.management.modules.common.audit.AuditUseCase.VC_SCHEMA_PUBLISHED;
import static ch.admin.bj.swiyu.trust.management.modules.common.persistence.TransactionManagerNames.MANAGEMENT_TRANSACTION_MANAGER;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import ch.admin.bit.jeap.audit.record.create.CreateAuditRecordCommand;
import ch.admin.bit.jeap.messaging.kafka.interceptor.JeapKafkaMessageCallback;
import ch.admin.bit.jeap.messaging.model.Message;
import ch.admin.bit.jeap.messaging.transactionaloutbox.outbox.DeferredMessageRepository;
import ch.admin.bit.jeap.security.test.WithJeapAuthenticationToken;
import ch.admin.bj.swiyu.messagetype.ti.TiVcSchemaPublicationFailedEvent;
import ch.admin.bj.swiyu.messagetype.ti.TiVcSchemaPublicationSucceededEvent;
import ch.admin.bj.swiyu.trust.client.core.business.internal.api.VcSchemaSubmissionApi;
import ch.admin.bj.swiyu.trust.client.core.business.internal.model.VcSchemaSubmissionDto;
import ch.admin.bj.swiyu.trust.management.modules.common.audit.AuditMapper;
import ch.admin.bj.swiyu.trust.management.modules.common.audit.AuditUseCase;
import ch.admin.bj.swiyu.trust.management.modules.management.domain.event.TiVcSchemaSubmissionAcceptedEventBuilder;
import ch.admin.bj.swiyu.trust.management.modules.management.service.VcSchemaSubmissionEventProcessor;
import ch.admin.bj.swiyu.trust.management.test.PostgreSQLContainerInitializer;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.kafka.test.context.EmbeddedKafka;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;
import org.testcontainers.junit.jupiter.Testcontainers;

@ActiveProfiles("test")
@SpringBootTest
@EmbeddedKafka
@AutoConfigureMockMvc
@Testcontainers
@ContextConfiguration(initializers = PostgreSQLContainerInitializer.class)
class VcSchemaSubmissionEventProcessorIT {

    @Autowired
    VcSchemaSubmissionEventProcessor processor;

    @Autowired
    DeferredMessageRepository deferredMessageRepository;

    @MockitoBean
    VcSchemaSubmissionApi vcSchemaSubmissionInternalApiApi;

    @MockitoBean
    JeapKafkaMessageCallback kafkaMsgCallback;

    @Autowired
    @Qualifier(MANAGEMENT_TRANSACTION_MANAGER)
    PlatformTransactionManager managementTransactionManager;

    @BeforeEach
    void setUp() {
        reset(kafkaMsgCallback);
        new TransactionTemplate(managementTransactionManager).executeWithoutResult(_ ->
            clearDeferredMessages(deferredMessageRepository)
        );
    }

    @Test
    @WithJeapAuthenticationToken(username = "test")
    void shouldPublishSucceededEvent_whenSchemaPublicationSucceeds() {
        var submissionId = UUID.randomUUID();
        var partnerId = UUID.randomUUID();
        var submission = new VcSchemaSubmissionDto()
            .id(submissionId)
            .version(1L)
            ._file("{\"vct\":\"https://trust-reg.trust-infra.swiyu.admin.ch/api/v1/vc-schema/mySchema\"}")
            .partnerId(partnerId);

        when(vcSchemaSubmissionInternalApiApi.getVcSchemaSubmission(submissionId)).thenReturn(submission);

        var event = TiVcSchemaSubmissionAcceptedEventBuilder.create().vcSchemaSubmissionId(submissionId).build();

        processor.processVcSchemaSubmissionAccepted(event);

        var sentMessages = captureAllSentMessages(kafkaMsgCallback, 2);
        verifyTiVcSchemaPublicationSucceededEventWasSent(sentMessages, submissionId);
        verifyVcSchemaPublishedAudit(sentMessages, submissionId, partnerId, submission);
        assertThat(deferredMessageRepository.findAll()).hasSizeGreaterThanOrEqualTo(2);
    }

    @Test
    void shouldPublishFailedEvent_whenSchemaPublicationFails() {
        var submissionId = UUID.randomUUID();
        var submission = new VcSchemaSubmissionDto()
            .id(submissionId)
            .version(1L)
            ._file("{\"invalid\":true}")
            .partnerId(UUID.randomUUID());

        when(vcSchemaSubmissionInternalApiApi.getVcSchemaSubmission(submissionId)).thenReturn(submission);

        var event = TiVcSchemaSubmissionAcceptedEventBuilder.create().vcSchemaSubmissionId(submissionId).build();
        //        messagingSecurityContext.setPreferredUser(event.getPublisher());

        processor.processVcSchemaSubmissionAccepted(event);

        verifyTiVcSchemaPublicationFailedEventWasSent(captureAllSentMessages(kafkaMsgCallback, 1), submissionId);
    }

    private static void verifyTiVcSchemaPublicationSucceededEventWasSent(
        List<Message> sentMessages,
        UUID vcSchemaSubmissionId
    ) {
        var msg = sentMessages
            .stream()
            .filter(TiVcSchemaPublicationSucceededEvent.class::isInstance)
            .map(TiVcSchemaPublicationSucceededEvent.class::cast)
            .findFirst()
            .orElseThrow();
        assertThat(msg.getOptionalPayload().orElseThrow().getVcSchemaSubmissionId()).isEqualTo(vcSchemaSubmissionId);
    }

    private static void verifyTiVcSchemaPublicationFailedEventWasSent(
        List<Message> sentMessages,
        UUID vcSchemaSubmissionId
    ) {
        assertThat(sentMessages).hasSize(1).first().isInstanceOf(TiVcSchemaPublicationFailedEvent.class);
        var msg = (TiVcSchemaPublicationFailedEvent) sentMessages.getFirst();
        assertThat(msg.getOptionalPayload().orElseThrow().getVcSchemaSubmissionId()).isEqualTo(vcSchemaSubmissionId);
        assertThat(sentMessages).noneMatch(CreateAuditRecordCommand.class::isInstance);
    }

    private static List<Message> captureAllSentMessages(
        JeapKafkaMessageCallback kafkaMsgCallback,
        int expectedSendCount
    ) {
        var captor = ArgumentCaptor.forClass(Message.class);
        verify(kafkaMsgCallback, times(expectedSendCount)).onSend(captor.capture(), any());
        return captor.getAllValues();
    }

    private static CreateAuditRecordCommand findByUseCase(
        List<CreateAuditRecordCommand> commands,
        AuditUseCase useCase
    ) {
        return commands
            .stream()
            .filter(cmd -> useCase.getName().equals(cmd.getPayload().getEvent().getContext().getUseCase()))
            .findFirst()
            .orElseThrow(() ->
                new AssertionError(
                    "No audit command for use case %s among %s".formatted(
                        useCase.getName(),
                        commands
                            .stream()
                            .map(c -> c.getPayload().getEvent().getContext().getUseCase())
                            .toList()
                    )
                )
            );
    }

    private static void verifyVcSchemaPublishedAudit(
        List<Message> sentMessages,
        UUID submissionId,
        UUID partnerId,
        VcSchemaSubmissionDto submission
    ) {
        var audit = findByUseCase(
            sentMessages
                .stream()
                .filter(CreateAuditRecordCommand.class::isInstance)
                .map(CreateAuditRecordCommand.class::cast)
                .toList(),
            VC_SCHEMA_PUBLISHED
        );
        assertThat(audit.getPayload().getEvent().getType()).isEqualTo(VC_SCHEMA_PUBLISHED.getEventType());
        assertAuditedObject(
            audit,
            submissionId.toString(),
            VC_SCHEMA_PUBLISHED.getAuditObjectType(),
            submission.getVersion().toString()
        );
        assertBusinessPartnerId(audit, partnerId.toString());
        assertJsonObjectData(audit, VC_SCHEMA_PUBLISHED.getMetaFieldName(), AuditMapper.toAuditJson(submission));
        assertValueObjectData(audit, VC_SCHEMA_PUBLISHED.getDataFieldName(), submission.getFile());
    }
}
