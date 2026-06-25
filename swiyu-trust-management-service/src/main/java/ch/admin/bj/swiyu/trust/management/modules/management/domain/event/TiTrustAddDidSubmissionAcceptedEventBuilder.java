package ch.admin.bj.swiyu.trust.management.modules.management.domain.event;

import ch.admin.bit.jeap.domainevent.avro.AvroDomainEventBuilder;
import ch.admin.bit.jeap.messaging.avro.AvroMessageBuilderException;
import ch.admin.bj.swiyu.messagetype.ti.TiTrustAddDidSubmissionAcceptedEvent;
import ch.admin.bj.swiyu.messagetype.ti.TrustAddDidSubmissionAcceptedPayload;
import java.util.UUID;

public class TiTrustAddDidSubmissionAcceptedEventBuilder
    extends AvroDomainEventBuilder<TiTrustAddDidSubmissionAcceptedEventBuilder, TiTrustAddDidSubmissionAcceptedEvent>
{

    private static final String SYSTEM_NAME = "swiyu-trust-management";
    private static final String SERVICE_NAME = "swiyu-trust-management-service";

    private UUID trustAddDidSubmissionId;

    private boolean isIdempotenceIdOverwritten;

    private TiTrustAddDidSubmissionAcceptedEventBuilder() {
        super(TiTrustAddDidSubmissionAcceptedEvent::new);
    }

    public static TiTrustAddDidSubmissionAcceptedEventBuilder create() {
        return new TiTrustAddDidSubmissionAcceptedEventBuilder();
    }

    public TiTrustAddDidSubmissionAcceptedEventBuilder trustAddDidSubmissionId(UUID trustAddDidSubmissionId) {
        this.trustAddDidSubmissionId = trustAddDidSubmissionId;
        return this;
    }

    @Override
    public TiTrustAddDidSubmissionAcceptedEventBuilder idempotenceId(String idempotenceId) {
        this.isIdempotenceIdOverwritten = true;
        return super.idempotenceId(idempotenceId);
    }

    @Override
    protected String getServiceName() {
        return SERVICE_NAME;
    }

    @Override
    protected String getSystemName() {
        return SYSTEM_NAME;
    }

    @Override
    protected TiTrustAddDidSubmissionAcceptedEventBuilder self() {
        return this;
    }

    @Override
    public TiTrustAddDidSubmissionAcceptedEvent build() {
        if (!isIdempotenceIdOverwritten) {
            super.idempotenceId(UUID.randomUUID().toString());
        }
        if (this.trustAddDidSubmissionId == null) {
            throw AvroMessageBuilderException.propertyNull("payload.trustAddDidSubmissionId");
        }
        TrustAddDidSubmissionAcceptedPayload payload = TrustAddDidSubmissionAcceptedPayload.newBuilder()
            .setTrustAddDidSubmissionId(trustAddDidSubmissionId)
            .build();
        setPayload(payload);
        return super.build();
    }
}
