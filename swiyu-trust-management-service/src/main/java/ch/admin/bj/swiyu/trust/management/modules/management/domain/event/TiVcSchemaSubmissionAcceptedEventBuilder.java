package ch.admin.bj.swiyu.trust.management.modules.management.domain.event;

import ch.admin.bit.jeap.domainevent.avro.AvroDomainEventBuilder;
import ch.admin.bit.jeap.messaging.avro.AvroMessageBuilderException;
import ch.admin.bj.swiyu.messagetype.ti.*;
import java.util.UUID;

public class TiVcSchemaSubmissionAcceptedEventBuilder
    extends AvroDomainEventBuilder<TiVcSchemaSubmissionAcceptedEventBuilder, TiVcSchemaSubmissionAcceptedEvent>
{

    private static final String SYSTEM_NAME = "swiyu-core-business-service";
    private static final String SERVICE_NAME = "swiyu-core";

    private UUID vcSchemaSubmissionId;

    private boolean isIdempotenceIdOverwritten;

    private TiVcSchemaSubmissionAcceptedEventBuilder() {
        super(TiVcSchemaSubmissionAcceptedEvent::new);
    }

    public static TiVcSchemaSubmissionAcceptedEventBuilder create() {
        return new TiVcSchemaSubmissionAcceptedEventBuilder();
    }

    public TiVcSchemaSubmissionAcceptedEventBuilder vcSchemaSubmissionId(UUID vcSchemaSubmissionId) {
        this.vcSchemaSubmissionId = vcSchemaSubmissionId;
        return this;
    }

    @Override
    public TiVcSchemaSubmissionAcceptedEventBuilder idempotenceId(String idempotenceId) {
        isIdempotenceIdOverwritten = true;
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
    protected TiVcSchemaSubmissionAcceptedEventBuilder self() {
        return this;
    }

    @Override
    public TiVcSchemaSubmissionAcceptedEvent build() {
        if (!isIdempotenceIdOverwritten) {
            super.idempotenceId(UUID.randomUUID().toString());
        }
        if (this.vcSchemaSubmissionId == null) {
            throw AvroMessageBuilderException.propertyNull("declarationReferences.vcSchemaSubmissionId");
        }
        VcSchemaSubmissionAcceptedReferences declarationReferences =
            VcSchemaSubmissionAcceptedReferences.newBuilder().build();
        VcSchemaSubmissionAcceptedPayload declarationPayload = VcSchemaSubmissionAcceptedPayload.newBuilder()
            .setVcSchemaSubmissionId(vcSchemaSubmissionId)
            .build();
        setReferences(declarationReferences);
        setPayload(declarationPayload);
        return super.build();
    }
}
