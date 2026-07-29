package ch.admin.bj.swiyu.trust.management.modules.management.domain.event;

import ch.admin.bit.jeap.domainevent.avro.AvroDomainEventBuilder;
import ch.admin.bit.jeap.messaging.avro.AvroMessageBuilderException;
import ch.admin.bj.swiyu.messagetype.ti.BusinessPartnerIdentityActivatedPayload;
import ch.admin.bj.swiyu.messagetype.ti.BusinessPartnerIdentityStatus;
import ch.admin.bj.swiyu.messagetype.ti.TiBusinessPartnerIdentityActivatedEvent;
import ch.admin.bj.swiyu.trust.management.modules.management.domain.BusinessPartnerIdentity;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public class TiBusinessPartnerIdentityActivatedEventBuilder
    extends AvroDomainEventBuilder<
        TiBusinessPartnerIdentityActivatedEventBuilder,
        TiBusinessPartnerIdentityActivatedEvent
    >
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

    private TiBusinessPartnerIdentityActivatedEventBuilder() {
        super(TiBusinessPartnerIdentityActivatedEvent::new);
    }

    public static TiBusinessPartnerIdentityActivatedEventBuilder create() {
        return new TiBusinessPartnerIdentityActivatedEventBuilder();
    }

    public TiBusinessPartnerIdentityActivatedEventBuilder businessPartnerIdentity(
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

    public TiBusinessPartnerIdentityActivatedEventBuilder businessPartnerIdentityId(UUID businessPartnerIdentityId) {
        this.businessPartnerIdentityId = businessPartnerIdentityId;
        return this;
    }

    public TiBusinessPartnerIdentityActivatedEventBuilder validUntil(Instant validUntil) {
        this.validUntil = validUntil;
        return this;
    }

    public TiBusinessPartnerIdentityActivatedEventBuilder trustedIdentifier(List<String> trustedIdentifier) {
        this.trustedIdentifier = trustedIdentifier;
        return this;
    }

    public TiBusinessPartnerIdentityActivatedEventBuilder status(BusinessPartnerIdentityStatus status) {
        this.status = status;
        return this;
    }

    public TiBusinessPartnerIdentityActivatedEventBuilder lastActivated(Instant lastActivated) {
        this.lastActivated = lastActivated;
        return this;
    }

    public TiBusinessPartnerIdentityActivatedEventBuilder uid(String uid) {
        this.uid = uid;
        return this;
    }

    public TiBusinessPartnerIdentityActivatedEventBuilder entityName(Map<String, String> entityName) {
        this.entityName = entityName;
        return this;
    }

    public TiBusinessPartnerIdentityActivatedEventBuilder version(Long version) {
        this.version = version;
        return this;
    }

    @Override
    public TiBusinessPartnerIdentityActivatedEventBuilder idempotenceId(String idempotenceId) {
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
    protected TiBusinessPartnerIdentityActivatedEventBuilder self() {
        return this;
    }

    @Override
    public TiBusinessPartnerIdentityActivatedEvent build() {
        if (!isIdempotenceIdOverwritten) {
            super.idempotenceId(UUID.randomUUID().toString());
        }

        if (this.businessPartnerIdentityId == null) {
            throw AvroMessageBuilderException.propertyNull(
                "declarationPayload.TiBusinessPartnerIdentityActivatedEvent"
            );
        }
        if (this.validUntil == null) {
            throw AvroMessageBuilderException.propertyNull("declarationPayload.validUntil");
        }
        if (this.status == null) {
            throw AvroMessageBuilderException.propertyNull("declarationPayload.status");
        }
        if (this.lastActivated == null) {
            throw AvroMessageBuilderException.propertyNull("declarationPayload.lastActivated");
        }
        if (this.entityName == null) {
            throw AvroMessageBuilderException.propertyNull("declarationPayload.entityName");
        }

        var declarationPayload = BusinessPartnerIdentityActivatedPayload.newBuilder()
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
