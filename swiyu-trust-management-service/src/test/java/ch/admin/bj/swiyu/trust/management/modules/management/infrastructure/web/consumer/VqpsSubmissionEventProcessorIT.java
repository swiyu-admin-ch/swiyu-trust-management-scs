package ch.admin.bj.swiyu.trust.management.modules.management.infrastructure.web.consumer;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

import ch.admin.bit.jeap.messaging.kafka.interceptor.JeapKafkaMessageCallback;
import ch.admin.bit.jeap.security.test.WithJeapAuthenticationToken;
import ch.admin.bj.swiyu.messagetype.ti.TiVqpsPublicationSucceededEvent;
import ch.admin.bj.swiyu.trust.client.core.business.internal.api.VqpsSubmissionInternalApi;
import ch.admin.bj.swiyu.trust.client.core.business.internal.model.VqpsSubmissionInternalDto;
import ch.admin.bj.swiyu.trust.management.modules.management.domain.event.TiVqpsSubmissionAcceptedEventBuilder;
import ch.admin.bj.swiyu.trust.management.modules.management.service.VqpsSubmissionEventProcessor;
import ch.admin.bj.swiyu.trust.management.test.PostgreSQLContainerInitializer;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpStatus;
import org.springframework.kafka.test.context.EmbeddedKafka;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.web.client.HttpServerErrorException;
import org.testcontainers.junit.jupiter.Testcontainers;

@ActiveProfiles("test")
@SpringBootTest
@EmbeddedKafka
@AutoConfigureMockMvc
@Testcontainers
@ContextConfiguration(initializers = PostgreSQLContainerInitializer.class)
class VqpsSubmissionEventProcessorIT {

    @Autowired
    VqpsSubmissionEventProcessor processor;

    @MockitoBean
    JeapKafkaMessageCallback kafkaMsgCallback;

    @MockitoBean
    VqpsSubmissionInternalApi vqpsSubmissionInternalApi;

    @Test
    @WithJeapAuthenticationToken(username = "test")
    void shouldPublishSucceededEvent_whenSubmissionPublicationSucceeds() {
        // given
        var submissionId = UUID.randomUUID();
        when(vqpsSubmissionInternalApi.getVqpsSubmission(submissionId)).thenReturn(validSubmission(submissionId));
        var event = TiVqpsSubmissionAcceptedEventBuilder.create().vqpsSubmissionId(submissionId).build();

        // when
        processor.processVqpsSubmissionAccepted(event);

        // then
        verifySucceededEventSent(submissionId);
    }

    @Test
    @WithJeapAuthenticationToken(username = "test")
    void shouldPropagateException_whenCbsApiFails() {
        // given
        var submissionId = UUID.randomUUID();
        var cbsError = HttpServerErrorException.create(
            HttpStatus.SERVICE_UNAVAILABLE,
            "CBS unavailable",
            null,
            null,
            null
        );
        when(vqpsSubmissionInternalApi.getVqpsSubmission(submissionId)).thenThrow(cbsError);
        var event = TiVqpsSubmissionAcceptedEventBuilder.create().vqpsSubmissionId(submissionId).build();

        // when / then — exception bubbles so Kafka redelivers; no event is published
        assertThatThrownBy(() -> processor.processVqpsSubmissionAccepted(event)).isSameAs(cbsError);
        verify(kafkaMsgCallback, never()).onSend(any(), any());
    }

    @Test
    @WithJeapAuthenticationToken(username = "test")
    void shouldNotReprocessEvent_whenSameEventDeliveredTwice() {
        // given
        var submissionId = UUID.randomUUID();
        when(vqpsSubmissionInternalApi.getVqpsSubmission(submissionId)).thenReturn(validSubmission(submissionId));
        var event = TiVqpsSubmissionAcceptedEventBuilder.create()
            .vqpsSubmissionId(submissionId)
            .idempotenceId("fixed-idempotence-id-" + submissionId)
            .build();

        // when — same event delivered twice
        processor.processVqpsSubmissionAccepted(event);
        processor.processVqpsSubmissionAccepted(event);

        // then — CBS fetched only once, only one succeeded event sent
        verify(vqpsSubmissionInternalApi, times(1)).getVqpsSubmission(submissionId);
        verify(kafkaMsgCallback, times(1)).onSend(any(TiVqpsPublicationSucceededEvent.class), any());
    }

    private VqpsSubmissionInternalDto validSubmission(UUID submissionId) {
        return new VqpsSubmissionInternalDto()
            .id(submissionId)
            .partnerId(UUID.randomUUID())
            .sub("did:example:subject-" + submissionId)
            .purposeName(Map.of("default", "Purpose", "en", "Purpose EN"))
            .purposeDescription(Map.of("default", "Description", "en", "Description EN"))
            .scope("some-scope")
            .query(Map.of("credentials", List.of()));
    }

    private void verifySucceededEventSent(UUID submissionId) {
        var captor = ArgumentCaptor.forClass(TiVqpsPublicationSucceededEvent.class);
        verify(kafkaMsgCallback, times(1)).onSend(captor.capture(), any());
        assertThat(captor.getValue().getOptionalPayload().orElseThrow().getVqpsSubmissionId()).isEqualTo(submissionId);
    }
}
