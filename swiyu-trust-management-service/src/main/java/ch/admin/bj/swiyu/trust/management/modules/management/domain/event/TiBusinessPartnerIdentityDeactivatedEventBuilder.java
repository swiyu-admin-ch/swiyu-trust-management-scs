package ch.admin.bj.swiyu.trust.management.modules.management.domain.event;

import ch.admin.bit.jeap.domainevent.avro.AvroDomainEventBuilder;
import ch.admin.bit.jeap.messaging.avro.AvroMessageBuilderException;
import ch.admin.bj.swiyu.messagetype.ti.BusinessPartnerIdentityDeactivatedPayload;
import ch.admin.bj.swiyu.messagetype.ti.BusinessPartnerIdentityStatus;
import ch.admin.bj.swiyu.messagetype.ti.TiBusinessPartnerIdentityDeactivatedEvent;
import ch.admin.bj.swiyu.trust.management.modules.management.domain.BusinessPartnerIdentity;
import java.util.UUID;

public class TiBusinessPartnerIdentityDeactivatedEventBuilder
    extends AvroDomainEventBuilder<
        TiBusinessPartnerIdentityDeactivatedEventBuilder,
        TiBusinessPartnerIdentityDeactivatedEvent
    >
{

    private static final String SYSTEM_NAME = "swiyu-trust-management";
    private static final String SERVICE_NAME = "swiyu-trust-management-service";

    private UUID businessPartnerIdentityId;
    private BusinessPartnerIdentityStatus status;
    private long version;
    private boolean isIdempotenceIdOverwritten;

    private TiBusinessPartnerIdentityDeactivatedEventBuilder() {
        super(TiBusinessPartnerIdentityDeactivatedEvent::new);
    }

    public static TiBusinessPartnerIdentityDeactivatedEventBuilder create() {
        return new TiBusinessPartnerIdentityDeactivatedEventBuilder();
    }

    public TiBusinessPartnerIdentityDeactivatedEventBuilder businessPartnerIdentity(
        BusinessPartnerIdentity businessPartnerIdentityId
    ) {
        this.businessPartnerIdentityId = businessPartnerIdentityId.getId();
        this.status = businessPartnerIdentityId.getStatus();
        this.version = businessPartnerIdentityId.getVersion();
        return this;
    }

    public TiBusinessPartnerIdentityDeactivatedEventBuilder businessPartnerIdentityId(UUID businessPartnerIdentityId) {
        this.businessPartnerIdentityId = businessPartnerIdentityId;
        return this;
    }

    public TiBusinessPartnerIdentityDeactivatedEventBuilder status(BusinessPartnerIdentityStatus status) {
        this.status = status;
        return this;
    }

    public TiBusinessPartnerIdentityDeactivatedEventBuilder version(Long version) {
        this.version = version;
        return this;
    }

    @Override
    public TiBusinessPartnerIdentityDeactivatedEventBuilder idempotenceId(String idempotenceId) {
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
    protected TiBusinessPartnerIdentityDeactivatedEventBuilder self() {
        return this;
    }

    @Override
    public TiBusinessPartnerIdentityDeactivatedEvent build() {
        if (!isIdempotenceIdOverwritten) {
            super.idempotenceId(UUID.randomUUID().toString());
        }
        if (this.businessPartnerIdentityId == null) {
            throw AvroMessageBuilderException.propertyNull("declarationPayload.businessPartnerIdentityId");
        }
        if (this.status == null) {
            throw AvroMessageBuilderException.propertyNull("declarationPayload.status");
        }

        var declarationPayload = BusinessPartnerIdentityDeactivatedPayload.newBuilder()
            .setBusinessPartnerIdentityId(businessPartnerIdentityId)
            .setStatus(status)
            .setVersion(version)
            .build();
        setPayload(declarationPayload);
        return super.build();
    }
}
