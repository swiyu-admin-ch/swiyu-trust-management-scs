package ch.admin.bj.swiyu.trust.management.modules.management.domain.event;

import ch.admin.bit.jeap.domainevent.avro.AvroDomainEventBuilder;
import ch.admin.bit.jeap.messaging.avro.AvroMessageBuilderException;
import ch.admin.bj.swiyu.messagetype.ti.*;
import java.util.UUID;

public class TiTrustOnboardingInformationRequestedEventBuilder
    extends AvroDomainEventBuilder<
        TiTrustOnboardingInformationRequestedEventBuilder,
        TiTrustOnboardingInformationRequestedEvent
    >
{

    private static final String SYSTEM_NAME = "swiyu-trust-management";
    private static final String SERVICE_NAME = "swiyu-trust-management-service";

    private UUID trustOnboardingSubmissionId;
    private String partnerNote;
    private String declineReasonType;

    private boolean isIdempotenceIdOverwritten;

    private TiTrustOnboardingInformationRequestedEventBuilder() {
        super(TiTrustOnboardingInformationRequestedEvent::new);
    }

    public static TiTrustOnboardingInformationRequestedEventBuilder create() {
        return new TiTrustOnboardingInformationRequestedEventBuilder();
    }

    public TiTrustOnboardingInformationRequestedEventBuilder trustOnboardingSubmissionId(
        UUID trustOnboardingSubmissionId
    ) {
        this.trustOnboardingSubmissionId = trustOnboardingSubmissionId;
        return this;
    }

    public TiTrustOnboardingInformationRequestedEventBuilder partnerNote(String partnerNote) {
        this.partnerNote = partnerNote;
        return this;
    }

    public TiTrustOnboardingInformationRequestedEventBuilder declineReasonType(String declineReasonType) {
        this.declineReasonType = declineReasonType;
        return this;
    }

    @Override
    public TiTrustOnboardingInformationRequestedEventBuilder idempotenceId(String idempotenceId) {
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
    protected TiTrustOnboardingInformationRequestedEventBuilder self() {
        return this;
    }

    @Override
    public TiTrustOnboardingInformationRequestedEvent build() {
        if (!isIdempotenceIdOverwritten) {
            super.idempotenceId(UUID.randomUUID().toString());
        }
        if (this.trustOnboardingSubmissionId == null) {
            throw AvroMessageBuilderException.propertyNull(
                "rustOnboardingInformationRequestedReferences.trustOnboardingSubmissionId"
            );
        }
        TrustOnboardingInformationRequestedReferences trustOnboardingInformationRequestedReferences =
            TrustOnboardingInformationRequestedReferences.newBuilder().build();
        TrustOnboardingInformationRequestedPayload trustOnboardingInformationRequestedPayload =
            TrustOnboardingInformationRequestedPayload.newBuilder()
                .setTrustOnboardingSubmissionId(trustOnboardingSubmissionId)
                .setPartnerNote(partnerNote)
                .setRejectReason(declineReasonType)
                .build();
        setReferences(trustOnboardingInformationRequestedReferences);
        setPayload(trustOnboardingInformationRequestedPayload);
        return super.build();
    }
}
