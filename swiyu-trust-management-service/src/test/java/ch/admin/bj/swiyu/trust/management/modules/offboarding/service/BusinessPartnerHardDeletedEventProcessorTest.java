package ch.admin.bj.swiyu.trust.management.modules.offboarding.service;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.inOrder;

import ch.admin.bit.jeap.domainevent.avro.AvroDomainEventIdentity;
import ch.admin.bit.jeap.domainevent.avro.AvroDomainEventPublisher;
import ch.admin.bit.jeap.domainevent.avro.AvroDomainEventType;
import ch.admin.bj.swiyu.messagetype.ti.BusinessPartnerHardDeletedPayload;
import ch.admin.bj.swiyu.messagetype.ti.TiBusinessPartnerHardDeletedEvent;
import ch.admin.bj.swiyu.trust.management.modules.management.service.BusinessPartnerSyncService;
import java.time.Instant;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InOrder;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class BusinessPartnerHardDeletedEventProcessorTest {

    private static final UUID PARTNER_ID = UUID.randomUUID();

    @Mock
    private BusinessPartnerSyncService businessPartnerSyncService;

    @Mock
    private OffboardingService offboardingService;

    private BusinessPartnerHardDeletedEventProcessor processor;

    @BeforeEach
    void setUp() {
        processor = new BusinessPartnerHardDeletedEventProcessor(businessPartnerSyncService, offboardingService);
    }

    @Test
    void processBusinessPartnerHardDeletedEvent_syncsTheProjectionBeforeOffboarding() {
        processor.processBusinessPartnerHardDeletedEvent(event(PARTNER_ID));

        InOrder inOrder = inOrder(businessPartnerSyncService, offboardingService);
        inOrder.verify(businessPartnerSyncService).syncPartner(PARTNER_ID);
        inOrder.verify(offboardingService).hardDeleteBusinessPartner(PARTNER_ID);
    }

    @Test
    void processBusinessPartnerHardDeletedEvent_propagatesFailuresSoTheMessageIsNotAcknowledged() {
        doThrow(new IllegalStateException("offboarding failed"))
            .when(offboardingService)
            .hardDeleteBusinessPartner(PARTNER_ID);

        assertThatThrownBy(() -> processor.processBusinessPartnerHardDeletedEvent(event(PARTNER_ID))).isInstanceOf(
            IllegalStateException.class
        );
    }

    private static TiBusinessPartnerHardDeletedEvent event(UUID businessPartnerId) {
        var identity = AvroDomainEventIdentity.newBuilder()
            .setEventId(UUID.randomUUID().toString())
            .setIdempotenceId(UUID.randomUUID().toString())
            .setCreated(Instant.now())
            .build();

        var type = AvroDomainEventType.newBuilder()
            .setName("TiBusinessPartnerHardDeletedEvent")
            .setVersion("1.0.0")
            .build();

        var payload = BusinessPartnerHardDeletedPayload.newBuilder().setBusinessPartnerId(businessPartnerId).build();

        return TiBusinessPartnerHardDeletedEvent.newBuilder()
            .setIdentity(identity)
            .setType(type)
            .setPublisher(new AvroDomainEventPublisher())
            .setPayload(payload)
            .setDomainEventVersion("1.0.0")
            .build();
    }
}
