package ch.admin.bj.swiyu.trust.management.modules.management.service;

import static org.mockito.Mockito.verify;

import ch.admin.bit.jeap.domainevent.avro.AvroDomainEventIdentity;
import ch.admin.bit.jeap.domainevent.avro.AvroDomainEventPublisher;
import ch.admin.bit.jeap.domainevent.avro.AvroDomainEventType;
import ch.admin.bj.swiyu.messagetype.ti.BusinessPartnerUpdatedPayload;
import ch.admin.bj.swiyu.messagetype.ti.TiBusinessPartnerUpdatedEvent;
import java.time.Instant;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class BusinessPartnerEventProcessorTest {

    private static final UUID PARTNER_ID = UUID.randomUUID();

    @Mock
    private BusinessPartnerSyncService syncService;

    private BusinessPartnerEventProcessor processor;

    @BeforeEach
    void setUp() {
        processor = new BusinessPartnerEventProcessor(syncService);
    }

    @Test
    void processBusinessPartnerUpdatedEvent_syncsThePartnerNamedInTheEvent() {
        processor.processBusinessPartnerUpdatedEvent(event(PARTNER_ID));

        verify(syncService).syncPartner(PARTNER_ID);
    }

    private static TiBusinessPartnerUpdatedEvent event(UUID businessPartnerId) {
        var identity = AvroDomainEventIdentity.newBuilder()
            .setEventId(UUID.randomUUID().toString())
            .setIdempotenceId(UUID.randomUUID().toString())
            .setCreated(Instant.now())
            .build();

        var type = AvroDomainEventType.newBuilder()
            .setName("TiBusinessPartnerUpdatedEvent")
            .setVersion("1.0.0")
            .build();

        var payload = BusinessPartnerUpdatedPayload.newBuilder().setBusinessPartnerId(businessPartnerId).build();

        return TiBusinessPartnerUpdatedEvent.newBuilder()
            .setIdentity(identity)
            .setType(type)
            .setPublisher(new AvroDomainEventPublisher())
            .setPayload(payload)
            .setDomainEventVersion("1.0.0")
            .build();
    }
}
