package ch.admin.bj.swiyu.trust.management.modules.management.domain.event;

import ch.admin.bit.jeap.domainevent.avro.AvroDomainEventBuilder;
import ch.admin.bit.jeap.messaging.avro.AvroMessageBuilderException;
import ch.admin.bj.swiyu.messagetype.ti.TiTrustOnboardingSucceededEvent;
import ch.admin.bj.swiyu.messagetype.ti.TrustOnboardingSucceededPayload;
import ch.admin.bj.swiyu.messagetype.ti.TrustOnboardingSucceededReferences;
import java.util.UUID;

public class TiTrustOnboardingSucceededEventBuilder
    extends AvroDomainEventBuilder<TiTrustOnboardingSucceededEventBuilder, TiTrustOnboardingSucceededEvent>
{

    private static final String SYSTEM_NAME = "swiyu-trust-management";
    private static final String SERVICE_NAME = "swiyu-trust-management-service";

    private UUID trustOnboardingSubmissionId;
    private String partnerNote;

    private boolean isIdempotenceIdOverwritten;

    private TiTrustOnboardingSucceededEventBuilder() {
        super(TiTrustOnboardingSucceededEvent::new);
    }

    public static TiTrustOnboardingSucceededEventBuilder create() {
        return new TiTrustOnboardingSucceededEventBuilder();
    }

    public TiTrustOnboardingSucceededEventBuilder trustOnboardingSubmissionId(UUID trustOnboardingSubmissionId) {
        this.trustOnboardingSubmissionId = trustOnboardingSubmissionId;
        return this;
    }

    public TiTrustOnboardingSucceededEventBuilder partnerNote(String partnerNote) {
        this.partnerNote = partnerNote;
        return this;
    }

    @Override
    public TiTrustOnboardingSucceededEventBuilder idempotenceId(String idempotenceId) {
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
    protected TiTrustOnboardingSucceededEventBuilder self() {
        return this;
    }

    @Override
    public TiTrustOnboardingSucceededEvent build() {
        if (!isIdempotenceIdOverwritten) {
            super.idempotenceId(UUID.randomUUID().toString());
        }
        if (this.trustOnboardingSubmissionId == null) {
            throw AvroMessageBuilderException.propertyNull("declarationReferences.trustOnboardingSubmissionId");
        }
        TrustOnboardingSucceededReferences declarationReferences =
            TrustOnboardingSucceededReferences.newBuilder().build();
        TrustOnboardingSucceededPayload declarationPayload = TrustOnboardingSucceededPayload.newBuilder()
            .setTrustOnboardingSubmissionId(trustOnboardingSubmissionId)
            .setPartnerNote(partnerNote)
            .build();
        setReferences(declarationReferences);
        setPayload(declarationPayload);
        return super.build();
    }
}
