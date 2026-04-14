package ch.admin.bj.swiyu.trust.management.modules.management.infrastructure.web.consumer;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

import ch.admin.bit.jeap.messaging.kafka.interceptor.JeapKafkaMessageCallback;
import ch.admin.bit.jeap.security.test.WithJeapAuthenticationToken;
import ch.admin.bj.swiyu.messagetype.ti.TiVcSchemaPublicationFailedEvent;
import ch.admin.bj.swiyu.messagetype.ti.TiVcSchemaPublicationSucceededEvent;
import ch.admin.bj.swiyu.trust.client.core.business.api.VcSchemaSubmissionApi;
import ch.admin.bj.swiyu.trust.client.core.business.model.VcSchemaSubmissionDto;
import ch.admin.bj.swiyu.trust.management.modules.management.domain.event.TiVcSchemaSubmissionAcceptedEventBuilder;
import ch.admin.bj.swiyu.trust.management.modules.management.service.VcSchemaSubmissionEventProcessor;
import ch.admin.bj.swiyu.trust.management.test.PostgreSQLContainerInitializer;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.kafka.test.context.EmbeddedKafka;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
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

    @MockitoBean
    VcSchemaSubmissionApi vcSchemaSubmissionInternalApiApi;

    @MockitoBean // registers a callback so we can verify the sent message
    JeapKafkaMessageCallback kafkaMsgCallback;

    @Test
    @WithJeapAuthenticationToken(username = "test")
    void shouldPublishSucceededEvent_whenSchemaPublicationSucceeds() {
        // given
        var submissionId = UUID.randomUUID();
        var submission = new VcSchemaSubmissionDto()
            .id(submissionId)
            ._file("{\"vct\":\"https://trust-reg.trust-infra.swiyu.admin.ch/api/v1/vc-schema/mySchema\"}")
            .partnerId(UUID.randomUUID());

        when(vcSchemaSubmissionInternalApiApi.getVcSchemaSubmission(submissionId)).thenReturn(submission);

        var event = TiVcSchemaSubmissionAcceptedEventBuilder.create().vcSchemaSubmissionId(submissionId).build();

        // when
        processor.processVcSchemaSubmissionAccepted(event);

        // then
        verifyTiVcSchemaPublicationSucceededEventWasSend(submissionId);
    }

    @Test
    @WithJeapAuthenticationToken(username = "test")
    void shouldPublishFailedEvent_whenSchemaPublicationFails() {
        // given
        var submissionId = UUID.randomUUID();
        var submission = new VcSchemaSubmissionDto()
            .id(submissionId)
            ._file("{\"invalid\":true}")
            .partnerId(UUID.randomUUID());

        when(vcSchemaSubmissionInternalApiApi.getVcSchemaSubmission(submissionId)).thenReturn(submission);

        var event = TiVcSchemaSubmissionAcceptedEventBuilder.create().vcSchemaSubmissionId(submissionId).build();

        // when
        processor.processVcSchemaSubmissionAccepted(event);

        // then
        verifyTiVcSchemaPublicationFailedEventWasSend(submissionId);
    }

    private void verifyTiVcSchemaPublicationSucceededEventWasSend(UUID vcSchemaSubmissionId) {
        var messageCaptor = ArgumentCaptor.forClass(TiVcSchemaPublicationSucceededEvent.class);
        verify(kafkaMsgCallback, times(1)).onSend(messageCaptor.capture(), any());
        var msg = messageCaptor.getValue();
        assertThat(msg.getOptionalPayload().orElseThrow().getVcSchemaSubmissionId()).isEqualTo(vcSchemaSubmissionId);
    }

    private void verifyTiVcSchemaPublicationFailedEventWasSend(UUID vcSchemaSubmissionId) {
        var messageCaptor = ArgumentCaptor.forClass(TiVcSchemaPublicationFailedEvent.class);
        verify(kafkaMsgCallback, times(1)).onSend(messageCaptor.capture(), any());
        var msg = messageCaptor.getValue();
        assertThat(msg.getOptionalPayload().orElseThrow().getVcSchemaSubmissionId()).isEqualTo(vcSchemaSubmissionId);
    }
}
