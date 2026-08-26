package ch.admin.bj.swiyu.trust.management.modules.management.domain.event;

import ch.admin.bit.jeap.domainevent.avro.AvroDomainEventBuilder;
import ch.admin.bit.jeap.messaging.avro.AvroMessageBuilderException;
import ch.admin.bj.swiyu.messagetype.ti.ProtectedVerificationSubmissionApprovedPayload;
import ch.admin.bj.swiyu.messagetype.ti.TiProtectedVerificationSubmissionApprovedEvent;
import java.util.UUID;

public class TiProtectedVerificationSubmissionApprovedEventBuilder
    extends AvroDomainEventBuilder<
        TiProtectedVerificationSubmissionApprovedEventBuilder,
        TiProtectedVerificationSubmissionApprovedEvent
    >
{

    private static final String SYSTEM_NAME = "swiyu-trust-management";
    private static final String SERVICE_NAME = "swiyu-trust-management-service";

    private UUID protectedVerificationSubmissionId;

    private boolean isIdempotenceIdOverwritten;

    private TiProtectedVerificationSubmissionApprovedEventBuilder() {
        super(TiProtectedVerificationSubmissionApprovedEvent::new);
    }

    public static TiProtectedVerificationSubmissionApprovedEventBuilder create() {
        return new TiProtectedVerificationSubmissionApprovedEventBuilder();
    }

    public TiProtectedVerificationSubmissionApprovedEventBuilder protectedVerificationSubmissionId(
        UUID protectedVerificationSubmissionId
    ) {
        this.protectedVerificationSubmissionId = protectedVerificationSubmissionId;
        return this;
    }

    @Override
    public TiProtectedVerificationSubmissionApprovedEventBuilder idempotenceId(String idempotenceId) {
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
    protected TiProtectedVerificationSubmissionApprovedEventBuilder self() {
        return this;
    }

    @Override
    public TiProtectedVerificationSubmissionApprovedEvent build() {
        if (!isIdempotenceIdOverwritten) {
            super.idempotenceId(UUID.randomUUID().toString());
        }
        if (this.protectedVerificationSubmissionId == null) {
            throw AvroMessageBuilderException.propertyNull("payload.protectedVerificationSubmissionId");
        }
        ProtectedVerificationSubmissionApprovedPayload payload =
            ProtectedVerificationSubmissionApprovedPayload.newBuilder()
                .setProtectedVerificationSubmissionId(protectedVerificationSubmissionId)
                .build();
        setPayload(payload);
        return super.build();
    }
}
