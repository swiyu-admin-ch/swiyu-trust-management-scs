package ch.admin.bj.swiyu.trust.management.modules.management.domain.event;

import ch.admin.bit.jeap.domainevent.avro.AvroDomainEventBuilder;
import ch.admin.bit.jeap.messaging.avro.AvroMessageBuilderException;
import ch.admin.bj.swiyu.messagetype.ti.BusinessPartnerIdentityStatus;
import ch.admin.bj.swiyu.messagetype.ti.BusinessPartnerIdentityUpdatedPayload;
import ch.admin.bj.swiyu.messagetype.ti.TiBusinessPartnerIdentityUpdatedEvent;
import ch.admin.bj.swiyu.trust.management.modules.management.domain.BusinessPartnerIdentity;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public class TiBusinessPartnerIdentityUpdatedEventBuilder
    extends AvroDomainEventBuilder<TiBusinessPartnerIdentityUpdatedEventBuilder, TiBusinessPartnerIdentityUpdatedEvent>
{

    private static final String SYSTEM_NAME = "swiyu-trust-management";
    private static final String SERVICE_NAME = "swiyu-trust-management-service";

    private UUID businessPartnerIdentityId;
    private Instant validUntil;
    private List<String> trustedIdentifier;
    private BusinessPartnerIdentityStatus status;
    private Instant lastActivated;
    private String uid;
    private Map<String, String> entityName;
    private long version;
    private boolean isIdempotenceIdOverwritten;

    private TiBusinessPartnerIdentityUpdatedEventBuilder() {
        super(TiBusinessPartnerIdentityUpdatedEvent::new);
    }

    public static TiBusinessPartnerIdentityUpdatedEventBuilder create() {
        return new TiBusinessPartnerIdentityUpdatedEventBuilder();
    }

    public TiBusinessPartnerIdentityUpdatedEventBuilder businessPartnerIdentity(
        BusinessPartnerIdentity businessPartnerIdentity
    ) {
        this.businessPartnerIdentityId = businessPartnerIdentity.getId();
        this.validUntil = businessPartnerIdentity.getValidUntil();
        this.trustedIdentifier = new ArrayList<>(businessPartnerIdentity.getTrustedIdentifier());
        this.status = businessPartnerIdentity.getStatus();
        this.lastActivated = businessPartnerIdentity.getLastActivated();
        this.uid = businessPartnerIdentity.getUid();
        this.entityName = businessPartnerIdentity.getEntityName();
        this.version = businessPartnerIdentity.getVersion();
        return this;
    }

    public TiBusinessPartnerIdentityUpdatedEventBuilder businessPartnerIdentityId(UUID businessPartnerIdentityId) {
        this.businessPartnerIdentityId = businessPartnerIdentityId;
        return this;
    }

    public TiBusinessPartnerIdentityUpdatedEventBuilder validUntil(Instant validUntil) {
        this.validUntil = validUntil;
        return this;
    }

    public TiBusinessPartnerIdentityUpdatedEventBuilder trustedIdentifier(List<String> trustedIdentifier) {
        this.trustedIdentifier = trustedIdentifier;
        return this;
    }

    public TiBusinessPartnerIdentityUpdatedEventBuilder status(BusinessPartnerIdentityStatus status) {
        this.status = status;
        return this;
    }

    public TiBusinessPartnerIdentityUpdatedEventBuilder lastActivated(Instant lastActivated) {
        this.lastActivated = lastActivated;
        return this;
    }

    public TiBusinessPartnerIdentityUpdatedEventBuilder uid(String uid) {
        this.uid = uid;
        return this;
    }

    public TiBusinessPartnerIdentityUpdatedEventBuilder entityName(Map<String, String> entityName) {
        this.entityName = entityName;
        return this;
    }

    public TiBusinessPartnerIdentityUpdatedEventBuilder version(Long version) {
        this.version = version;
        return this;
    }

    @Override
    public TiBusinessPartnerIdentityUpdatedEventBuilder idempotenceId(String idempotenceId) {
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
    protected TiBusinessPartnerIdentityUpdatedEventBuilder self() {
        return this;
    }

    @Override
    public TiBusinessPartnerIdentityUpdatedEvent build() {
        if (!isIdempotenceIdOverwritten) {
            super.idempotenceId(UUID.randomUUID().toString());
        }
        if (this.businessPartnerIdentityId == null) {
            throw AvroMessageBuilderException.propertyNull("declarationPayload.businessPartnerIdentityId");
        }
        if (this.trustedIdentifier == null) {
            throw AvroMessageBuilderException.propertyNull("declarationPayload.trustedIdentifier");
        }
        if (this.status == null) {
            throw AvroMessageBuilderException.propertyNull("declarationPayload.status");
        }
        if (this.entityName == null) {
            throw AvroMessageBuilderException.propertyNull("declarationPayload.entityName");
        }

        var declarationPayload = BusinessPartnerIdentityUpdatedPayload.newBuilder()
            .setBusinessPartnerIdentityId(businessPartnerIdentityId)
            .setValidUntil(validUntil)
            .setTrustedIdentifier(trustedIdentifier)
            .setStatus(status)
            .setLastActivated(lastActivated)
            .setUid(uid)
            .setEntityName(entityName)
            .setVersion(version)
            .build();
        setPayload(declarationPayload);
        return super.build();
    }
}
