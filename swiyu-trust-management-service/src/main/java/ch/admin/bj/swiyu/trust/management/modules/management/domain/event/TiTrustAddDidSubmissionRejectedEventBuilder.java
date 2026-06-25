package ch.admin.bj.swiyu.trust.management.modules.management.domain.event;

import ch.admin.bit.jeap.domainevent.avro.AvroDomainEventBuilder;
import ch.admin.bit.jeap.messaging.avro.AvroMessageBuilderException;
import ch.admin.bj.swiyu.messagetype.ti.RejectReason;
import ch.admin.bj.swiyu.messagetype.ti.TiTrustAddDidSubmissionRejectedEvent;
import ch.admin.bj.swiyu.messagetype.ti.TrustAddDidSubmissionRejectedPayload;
import java.util.UUID;

public class TiTrustAddDidSubmissionRejectedEventBuilder
    extends AvroDomainEventBuilder<TiTrustAddDidSubmissionRejectedEventBuilder, TiTrustAddDidSubmissionRejectedEvent>
{

    private static final String SYSTEM_NAME = "swiyu-trust-management";
    private static final String SERVICE_NAME = "swiyu-trust-management-service";

    private UUID trustAddDidSubmissionId;
    private RejectReason rejectReason;

    private boolean isIdempotenceIdOverwritten;

    private TiTrustAddDidSubmissionRejectedEventBuilder() {
        super(TiTrustAddDidSubmissionRejectedEvent::new);
    }

    public static TiTrustAddDidSubmissionRejectedEventBuilder create() {
        return new TiTrustAddDidSubmissionRejectedEventBuilder();
    }

    public TiTrustAddDidSubmissionRejectedEventBuilder trustAddDidSubmissionId(UUID trustAddDidSubmissionId) {
        this.trustAddDidSubmissionId = trustAddDidSubmissionId;
        return this;
    }

    public TiTrustAddDidSubmissionRejectedEventBuilder rejectReason(RejectReason rejectReason) {
        this.rejectReason = rejectReason;
        return this;
    }

    @Override
    public TiTrustAddDidSubmissionRejectedEventBuilder idempotenceId(String idempotenceId) {
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
    protected TiTrustAddDidSubmissionRejectedEventBuilder self() {
        return this;
    }

    @Override
    public TiTrustAddDidSubmissionRejectedEvent build() {
        if (!isIdempotenceIdOverwritten) {
            super.idempotenceId(UUID.randomUUID().toString());
        }
        if (this.trustAddDidSubmissionId == null) {
            throw AvroMessageBuilderException.propertyNull("payload.trustAddDidSubmissionId");
        }
        TrustAddDidSubmissionRejectedPayload payload = TrustAddDidSubmissionRejectedPayload.newBuilder()
            .setTrustAddDidSubmissionId(trustAddDidSubmissionId)
            .setRejectReason(rejectReason)
            .build();
        setPayload(payload);
        return super.build();
    }
}
