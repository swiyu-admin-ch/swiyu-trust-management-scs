package ch.admin.bj.swiyu.trust.management.modules.management.domain.event;

import ch.admin.bit.jeap.domainevent.avro.AvroDomainEventBuilder;
import ch.admin.bit.jeap.messaging.avro.AvroMessageBuilderException;
import ch.admin.bj.swiyu.messagetype.ti.*;
import java.util.UUID;

public class TiVqpsPublicationSucceededEventBuilder
    extends AvroDomainEventBuilder<TiVqpsPublicationSucceededEventBuilder, TiVqpsPublicationSucceededEvent>
{

    private static final String SYSTEM_NAME = "swiyu-trust-management";
    private static final String SERVICE_NAME = "swiyu-trust-management-service";

    private UUID vqpsSubmissionId;
    private String vqps;
    private boolean isIdempotenceIdOverwritten;

    private TiVqpsPublicationSucceededEventBuilder() {
        super(TiVqpsPublicationSucceededEvent::new);
    }

    public static TiVqpsPublicationSucceededEventBuilder create() {
        return new TiVqpsPublicationSucceededEventBuilder();
    }

    public TiVqpsPublicationSucceededEventBuilder vqpsSubmissionId(UUID vqpsSubmissionId) {
        this.vqpsSubmissionId = vqpsSubmissionId;
        return this;
    }

    public TiVqpsPublicationSucceededEventBuilder vqps(String vqps) {
        this.vqps = vqps;
        return this;
    }

    @Override
    public TiVqpsPublicationSucceededEventBuilder idempotenceId(String idempotenceId) {
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
    protected TiVqpsPublicationSucceededEventBuilder self() {
        return this;
    }

    @Override
    public TiVqpsPublicationSucceededEvent build() {
        if (!isIdempotenceIdOverwritten) {
            super.idempotenceId(UUID.randomUUID().toString());
        }
        if (this.vqpsSubmissionId == null) {
            throw AvroMessageBuilderException.propertyNull("declarationReferences.vqpsSubmissionId");
        }
        if (this.vqps == null) {
            throw AvroMessageBuilderException.propertyNull("declarationPayload.vqps");
        }
        VqpsPublicationSucceededPayload declarationPayload = VqpsPublicationSucceededPayload.newBuilder()
            .setVqpsSubmissionId(vqpsSubmissionId)
            .setVqps(vqps)
            .build();
        setPayload(declarationPayload);
        return super.build();
    }
}
