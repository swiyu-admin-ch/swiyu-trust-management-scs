package ch.admin.bj.swiyu.trust.management.modules.management.domain.command;

import ch.admin.bit.jeap.command.avro.AvroCommandBuilder;
import ch.admin.bit.jeap.messaging.avro.AvroMessageBuilderException;
import ch.admin.bj.swiyu.messagetype.ti.TiHardDeleteBusinessPartnerCommand;
import ch.admin.bj.swiyu.messagetype.ti.TiHardDeleteBusinessPartnerCommandPayload;
import ch.admin.bj.swiyu.messagetype.ti.common.AuditUser;
import java.util.UUID;

/**
 * Builds the command that orders the core business service to hard delete a partner. The trigger names the person who
 * requested the deletion in TMS: the job that approves the delete task only executes it, so a system trigger would lose
 * the originator the audit trail of the core business service has to name.
 */
public class TiHardDeleteBusinessPartnerCommandBuilder
    extends AvroCommandBuilder<TiHardDeleteBusinessPartnerCommandBuilder, TiHardDeleteBusinessPartnerCommand>
{

    private static final String SYSTEM_NAME = "swiyu-trust-management";
    private static final String SERVICE_NAME = "swiyu-trust-management-service";

    private UUID businessPartnerId;
    private String requesterId;
    private String requesterIdentityProvider;
    private boolean isIdempotenceIdOverwritten;

    private TiHardDeleteBusinessPartnerCommandBuilder() {
        super(TiHardDeleteBusinessPartnerCommand::new);
    }

    public static TiHardDeleteBusinessPartnerCommandBuilder create() {
        return new TiHardDeleteBusinessPartnerCommandBuilder();
    }

    public TiHardDeleteBusinessPartnerCommandBuilder businessPartnerId(UUID businessPartnerId) {
        this.businessPartnerId = businessPartnerId;
        return this;
    }

    /**
     * @param requesterId           audit user id of the person who requested the deletion, as stored in the task
     * @param requesterIdentityProvider issuer of that person's token, as configured for the resource server
     */
    public TiHardDeleteBusinessPartnerCommandBuilder requester(String requesterId, String requesterIdentityProvider) {
        this.requesterId = requesterId;
        this.requesterIdentityProvider = requesterIdentityProvider;
        return this;
    }

    @Override
    public TiHardDeleteBusinessPartnerCommandBuilder idempotenceId(String idempotenceId) {
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
    protected TiHardDeleteBusinessPartnerCommandBuilder self() {
        return this;
    }

    @Override
    public TiHardDeleteBusinessPartnerCommand build() {
        if (!isIdempotenceIdOverwritten) {
            super.idempotenceId(UUID.randomUUID().toString());
        }
        requireNonNull(businessPartnerId, "businessPartnerId");
        requireNonNull(requesterId, "requesterId");
        requireNonNull(requesterIdentityProvider, "requesterIdentityProvider");

        setPayload(
            TiHardDeleteBusinessPartnerCommandPayload.newBuilder()
                .setBusinessPartnerId(businessPartnerId)
                .setTrigger(
                    AuditUser.newBuilder().setId(requesterId).setIdentityProvider(requesterIdentityProvider).build()
                )
                .build()
        );
        return super.build();
    }

    private static void requireNonNull(Object value, String propertyName) {
        if (value == null) {
            throw AvroMessageBuilderException.propertyNull(propertyName);
        }
    }
}
