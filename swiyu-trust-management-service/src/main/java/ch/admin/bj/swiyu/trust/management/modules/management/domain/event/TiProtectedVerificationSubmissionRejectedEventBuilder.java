package ch.admin.bj.swiyu.trust.management.modules.management.domain.event;

import ch.admin.bit.jeap.domainevent.avro.AvroDomainEventBuilder;
import ch.admin.bit.jeap.messaging.avro.AvroMessageBuilderException;
import ch.admin.bj.swiyu.messagetype.ti.ProtectedVerificationSubmissionRejectedPayload;
import ch.admin.bj.swiyu.messagetype.ti.TiProtectedVerificationSubmissionRejectedEvent;
import java.util.UUID;

public class TiProtectedVerificationSubmissionRejectedEventBuilder
    extends AvroDomainEventBuilder<
        TiProtectedVerificationSubmissionRejectedEventBuilder,
        TiProtectedVerificationSubmissionRejectedEvent
    >
{

    private static final String SYSTEM_NAME = "swiyu-trust-management";
    private static final String SERVICE_NAME = "swiyu-trust-management-service";

    private UUID protectedVerificationSubmissionId;
    private String rejectReason;

    private boolean isIdempotenceIdOverwritten;

    private TiProtectedVerificationSubmissionRejectedEventBuilder() {
        super(TiProtectedVerificationSubmissionRejectedEvent::new);
    }

    public static TiProtectedVerificationSubmissionRejectedEventBuilder create() {
        return new TiProtectedVerificationSubmissionRejectedEventBuilder();
    }

    public TiProtectedVerificationSubmissionRejectedEventBuilder protectedVerificationSubmissionId(
        UUID protectedVerificationSubmissionId
    ) {
        this.protectedVerificationSubmissionId = protectedVerificationSubmissionId;
        return this;
    }

    public TiProtectedVerificationSubmissionRejectedEventBuilder rejectReason(String rejectReason) {
        this.rejectReason = rejectReason;
        return this;
    }

    @Override
    public TiProtectedVerificationSubmissionRejectedEventBuilder idempotenceId(String idempotenceId) {
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
    protected TiProtectedVerificationSubmissionRejectedEventBuilder self() {
        return this;
    }

    @Override
    public TiProtectedVerificationSubmissionRejectedEvent build() {
        if (!isIdempotenceIdOverwritten) {
            super.idempotenceId(UUID.randomUUID().toString());
        }
        if (this.protectedVerificationSubmissionId == null) {
            throw AvroMessageBuilderException.propertyNull("payload.protectedVerificationSubmissionId");
        }
        ProtectedVerificationSubmissionRejectedPayload payload =
            ProtectedVerificationSubmissionRejectedPayload.newBuilder()
                .setProtectedVerificationSubmissionId(protectedVerificationSubmissionId)
                .setRejectReason(rejectReason)
                .build();
        setPayload(payload);
        return super.build();
    }
}
