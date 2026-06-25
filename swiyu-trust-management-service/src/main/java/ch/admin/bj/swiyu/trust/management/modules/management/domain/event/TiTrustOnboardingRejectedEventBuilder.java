package ch.admin.bj.swiyu.trust.management.modules.management.domain.event;

import ch.admin.bit.jeap.domainevent.avro.AvroDomainEventBuilder;
import ch.admin.bit.jeap.messaging.avro.AvroMessageBuilderException;
import ch.admin.bj.swiyu.messagetype.ti.TiTrustOnboardingRejectedEvent;
import ch.admin.bj.swiyu.messagetype.ti.TrustOnboardingRejectedPayload;
import ch.admin.bj.swiyu.messagetype.ti.TrustOnboardingRejectedReferences;
import java.util.UUID;

public class TiTrustOnboardingRejectedEventBuilder
    extends AvroDomainEventBuilder<TiTrustOnboardingRejectedEventBuilder, TiTrustOnboardingRejectedEvent>
{

    private static final String SYSTEM_NAME = "swiyu-trust-management";
    private static final String SERVICE_NAME = "swiyu-trust-management-service";

    private UUID trustOnboardingSubmissionId;
    private String rejectReasonType;
    private String partnerNote;

    private boolean isIdempotenceIdOverwritten;

    private TiTrustOnboardingRejectedEventBuilder() {
        super(TiTrustOnboardingRejectedEvent::new);
    }

    public static TiTrustOnboardingRejectedEventBuilder create() {
        return new TiTrustOnboardingRejectedEventBuilder();
    }

    public TiTrustOnboardingRejectedEventBuilder trustOnboardingSubmissionId(UUID trustOnboardingSubmissionId) {
        this.trustOnboardingSubmissionId = trustOnboardingSubmissionId;
        return this;
    }

    public TiTrustOnboardingRejectedEventBuilder rejectReason(String rejectReasonType) {
        this.rejectReasonType = rejectReasonType;
        return this;
    }

    public TiTrustOnboardingRejectedEventBuilder partnerNote(String partnerNote) {
        this.partnerNote = partnerNote;
        return this;
    }

    @Override
    public TiTrustOnboardingRejectedEventBuilder idempotenceId(String idempotenceId) {
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
    protected TiTrustOnboardingRejectedEventBuilder self() {
        return this;
    }

    @Override
    public TiTrustOnboardingRejectedEvent build() {
        if (!isIdempotenceIdOverwritten) {
            super.idempotenceId(UUID.randomUUID().toString());
        }
        if (this.trustOnboardingSubmissionId == null) {
            throw AvroMessageBuilderException.propertyNull("declarationReferences.trustOnboardingSubmissionId");
        }
        if (this.rejectReasonType == null) {
            throw AvroMessageBuilderException.propertyNull("declarationReferences.rejectReasonType");
        }
        TrustOnboardingRejectedReferences declarationReferences =
            TrustOnboardingRejectedReferences.newBuilder().build();
        TrustOnboardingRejectedPayload declarationPayload = TrustOnboardingRejectedPayload.newBuilder()
            .setTrustOnboardingSubmissionId(trustOnboardingSubmissionId)
            .setRejectReason(rejectReasonType)
            .setPartnerNote(partnerNote)
            .build();
        setReferences(declarationReferences);
        setPayload(declarationPayload);
        return super.build();
    }
}
