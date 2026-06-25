package ch.admin.bj.swiyu.trust.management.modules.management.domain.publisher;

import static net.logstash.logback.argument.StructuredArguments.kv;

import ch.admin.bit.jeap.messaging.avro.AvroMessage;
import ch.admin.bit.jeap.messaging.transactionaloutbox.outbox.TransactionalOutbox;
import ch.admin.bj.swiyu.messagetype.ti.*;
import ch.admin.bj.swiyu.messagetype.ti.common.BeanReferenceMessageKey;
import lombok.AllArgsConstructor;
import lombok.NonNull;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

@Component
@AllArgsConstructor
@Slf4j
public class OutboxEventPublisher {

    public static final String EVENT_LOGGING_KEY = "event";
    public static final String TOPIC_LOGGING_KEY = "topic";

    public final TransactionalOutbox outbox;

    private void sendEvent(final String topicName, Object key, final AvroMessage event) {
        log.info("Publishing to topic {}", kv(TOPIC_LOGGING_KEY, topicName));
        outbox.sendMessage(event, key, topicName);
    }

    @Transactional(propagation = Propagation.MANDATORY)
    public void publishVcSchemaPublicationSucceededEvent(@NonNull TiVcSchemaPublicationSucceededEvent event) {
        var topicName = TiVcSchemaPublicationSucceededEvent.TypeRef.DEFAULT_TOPIC;
        sendEvent(
            topicName,
            BeanReferenceMessageKey.newBuilder()
                .setNamespace(TiVcSchemaPublicationSucceededEvent.TypeRef.SYSTEM_NAME)
                .setName(topicName)
                .setId(event.getPayload().getVcSchemaSubmissionId().toString())
                .build(),
            event
        );
    }

    @Transactional(propagation = Propagation.MANDATORY)
    public void publishVcSchemaPublicationFailedEvent(@NonNull TiVcSchemaPublicationFailedEvent event) {
        var topicName = TiVcSchemaPublicationFailedEvent.TypeRef.DEFAULT_TOPIC;
        sendEvent(
            topicName,
            BeanReferenceMessageKey.newBuilder()
                .setNamespace(TiVcSchemaPublicationFailedEvent.TypeRef.SYSTEM_NAME)
                .setName(topicName)
                .setId(event.getPayload().getVcSchemaSubmissionId().toString())
                .build(),
            event
        );
    }

    @Transactional(propagation = Propagation.MANDATORY)
    public void publishTrustOnboardingSucceededEvent(@NonNull TiTrustOnboardingSucceededEvent event) {
        var topicName = TiTrustOnboardingSucceededEvent.TypeRef.DEFAULT_TOPIC;
        sendEvent(
            topicName,
            BeanReferenceMessageKey.newBuilder()
                .setNamespace(TiTrustOnboardingSucceededEvent.TypeRef.SYSTEM_NAME)
                .setName(topicName)
                .setId(event.getPayload().getTrustOnboardingSubmissionId().toString())
                .build(),
            event
        );
    }

    @Transactional(propagation = Propagation.MANDATORY)
    public void publishTrustOnboardingRejectedEvent(@NonNull TiTrustOnboardingRejectedEvent event) {
        var topicName = TiTrustOnboardingRejectedEvent.TypeRef.DEFAULT_TOPIC;
        sendEvent(
            topicName,
            BeanReferenceMessageKey.newBuilder()
                .setNamespace(TiTrustOnboardingRejectedEvent.TypeRef.SYSTEM_NAME)
                .setName(topicName)
                .setId(event.getPayload().getTrustOnboardingSubmissionId().toString())
                .build(),
            event
        );
    }

    @Transactional(propagation = Propagation.MANDATORY)
    public void publishTrustOnboardingInformationRequestedEvent(
        @NonNull TiTrustOnboardingInformationRequestedEvent event
    ) {
        var topicName = TiTrustOnboardingInformationRequestedEvent.TypeRef.DEFAULT_TOPIC;
        sendEvent(
            topicName,
            BeanReferenceMessageKey.newBuilder()
                .setNamespace(TiTrustOnboardingInformationRequestedEvent.TypeRef.SYSTEM_NAME)
                .setName(topicName)
                .setId(event.getPayload().getTrustOnboardingSubmissionId().toString())
                .build(),
            event
        );
    }

    @Transactional(propagation = Propagation.MANDATORY)
    public void publishTrustAddDidSubmissionAcceptedEvent(@NonNull TiTrustAddDidSubmissionAcceptedEvent event) {
        var topicName = TiTrustAddDidSubmissionAcceptedEvent.TypeRef.DEFAULT_TOPIC;
        sendEvent(
            topicName,
            BeanReferenceMessageKey.newBuilder()
                .setNamespace(TiTrustAddDidSubmissionAcceptedEvent.TypeRef.SYSTEM_NAME)
                .setName(topicName)
                .setId(event.getPayload().getTrustAddDidSubmissionId().toString())
                .build(),
            event
        );
    }

    @Transactional(propagation = Propagation.MANDATORY)
    public void publishTrustAddDidSubmissionRejectedEvent(@NonNull TiTrustAddDidSubmissionRejectedEvent event) {
        var topicName = TiTrustAddDidSubmissionRejectedEvent.TypeRef.DEFAULT_TOPIC;
        sendEvent(
            topicName,
            BeanReferenceMessageKey.newBuilder()
                .setNamespace(TiTrustAddDidSubmissionRejectedEvent.TypeRef.SYSTEM_NAME)
                .setName(topicName)
                .setId(event.getPayload().getTrustAddDidSubmissionId().toString())
                .build(),
            event
        );
    }

    @Transactional(propagation = Propagation.MANDATORY)
    public void publishVqpsPublicationSucceededEvent(@NonNull TiVqpsPublicationSucceededEvent event) {
        var topicName = TiVqpsPublicationSucceededEvent.TypeRef.DEFAULT_TOPIC;
        sendEvent(
            topicName,
            BeanReferenceMessageKey.newBuilder()
                .setNamespace(TiVqpsPublicationSucceededEvent.TypeRef.SYSTEM_NAME)
                .setName(topicName)
                .setId(event.getPayload().getVqpsSubmissionId().toString())
                .build(),
            event
        );
    }
}
