package ch.admin.bj.swiyu.trust.management.modules.management.domain.event;

import ch.admin.bit.jeap.domainevent.avro.AvroDomainEventBuilder;
import ch.admin.bit.jeap.messaging.avro.AvroMessageBuilderException;
import ch.admin.bj.swiyu.messagetype.ti.*;
import java.util.UUID;

public class TiVcSchemaPublishingFailedEventBuilder
    extends AvroDomainEventBuilder<TiVcSchemaPublishingFailedEventBuilder, TiVcSchemaPublicationFailedEvent>
{

    private static final String SYSTEM_NAME = "swiyu-trust-management";
    private static final String SERVICE_NAME = "swiyu-trust-management-service";

    private UUID vcSchemaSubmissionId;
    private String failureReason;

    private boolean isIdempotenceIdOverwritten;

    private TiVcSchemaPublishingFailedEventBuilder() {
        super(TiVcSchemaPublicationFailedEvent::new);
    }

    public static TiVcSchemaPublishingFailedEventBuilder create() {
        return new TiVcSchemaPublishingFailedEventBuilder();
    }

    public TiVcSchemaPublishingFailedEventBuilder vcSchemaSubmissionId(UUID vcSchemaSubmissionId) {
        this.vcSchemaSubmissionId = vcSchemaSubmissionId;
        return this;
    }

    public TiVcSchemaPublishingFailedEventBuilder failureReason(String failureReason) {
        this.failureReason = failureReason;
        return this;
    }

    @Override
    public TiVcSchemaPublishingFailedEventBuilder idempotenceId(String idempotenceId) {
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
    protected TiVcSchemaPublishingFailedEventBuilder self() {
        return this;
    }

    @Override
    public TiVcSchemaPublicationFailedEvent build() {
        if (!isIdempotenceIdOverwritten) {
            super.idempotenceId(UUID.randomUUID().toString());
        }
        if (this.vcSchemaSubmissionId == null) {
            throw AvroMessageBuilderException.propertyNull("declarationReferences.vcSchemaSubmissionId");
        }
        if (this.failureReason == null) {
            throw AvroMessageBuilderException.propertyNull("declarationPayload.failureReason");
        }
        VcSchemaPublicationFailedReferences declarationReferences =
            VcSchemaPublicationFailedReferences.newBuilder().build();
        VcSchemaPublicationFailedPayload declarationPayload = VcSchemaPublicationFailedPayload.newBuilder()
            .setVcSchemaSubmissionId(vcSchemaSubmissionId)
            .setFailureReason(failureReason)
            .build();
        setReferences(declarationReferences);
        setPayload(declarationPayload);
        return super.build();
    }
}
