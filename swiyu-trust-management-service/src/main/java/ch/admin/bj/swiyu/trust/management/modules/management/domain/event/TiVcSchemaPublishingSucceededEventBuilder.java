package ch.admin.bj.swiyu.trust.management.modules.management.domain.event;

import ch.admin.bit.jeap.domainevent.avro.AvroDomainEventBuilder;
import ch.admin.bit.jeap.messaging.avro.AvroMessageBuilderException;
import ch.admin.bj.swiyu.messagetype.ti.*;
import java.util.UUID;

public class TiVcSchemaPublishingSucceededEventBuilder
    extends AvroDomainEventBuilder<TiVcSchemaPublishingSucceededEventBuilder, TiVcSchemaPublicationSucceededEvent>
{

    private static final String SYSTEM_NAME = "swiyu-trust-management";
    private static final String SERVICE_NAME = "swiyu-trust-management-service";

    private UUID vcSchemaSubmissionId;

    private boolean isIdempotenceIdOverwritten;

    private TiVcSchemaPublishingSucceededEventBuilder() {
        super(TiVcSchemaPublicationSucceededEvent::new);
    }

    public static TiVcSchemaPublishingSucceededEventBuilder create() {
        return new TiVcSchemaPublishingSucceededEventBuilder();
    }

    public TiVcSchemaPublishingSucceededEventBuilder vcSchemaSubmissionId(UUID vcSchemaSubmissionId) {
        this.vcSchemaSubmissionId = vcSchemaSubmissionId;
        return this;
    }

    @Override
    public TiVcSchemaPublishingSucceededEventBuilder idempotenceId(String idempotenceId) {
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
    protected TiVcSchemaPublishingSucceededEventBuilder self() {
        return this;
    }

    @Override
    public TiVcSchemaPublicationSucceededEvent build() {
        if (!isIdempotenceIdOverwritten) {
            super.idempotenceId(UUID.randomUUID().toString());
        }
        if (this.vcSchemaSubmissionId == null) {
            throw AvroMessageBuilderException.propertyNull("declarationReferences.vcSchemaSubmissionId");
        }
        VcSchemaPublicationSucceededReferences declarationReferences =
            VcSchemaPublicationSucceededReferences.newBuilder().build();
        VcSchemaPublicationSucceededPayload declarationPayload = VcSchemaPublicationSucceededPayload.newBuilder()
            .setVcSchemaSubmissionId(vcSchemaSubmissionId)
            .build();
        setReferences(declarationReferences);
        setPayload(declarationPayload);
        return super.build();
    }
}
