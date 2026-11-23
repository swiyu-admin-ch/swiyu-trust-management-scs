package ch.admin.bj.swiyu.trust.management.modules.management.domain.command;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import ch.admin.bit.jeap.messaging.avro.AvroMessageBuilderException;
import ch.admin.bj.swiyu.messagetype.ti.common.AuditUser;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class TiHardDeleteBusinessPartnerCommandBuilderTest {

    private static final UUID PARTNER_ID = UUID.randomUUID();
    private static final String REQUESTER_ID = "47443696:Mustermann,Max";
    private static final String ISSUER = "https://issuer.test/realms/swiyu";

    @Test
    void build_carriesThePartnerAndTheRequesterAsTrigger() {
        var command = builder().build();

        assertThat(command.getPayload().getBusinessPartnerId()).isEqualTo(PARTNER_ID);
        assertThat(command.getPayload().getTrigger()).isInstanceOf(AuditUser.class);
        var requester = (AuditUser) command.getPayload().getTrigger();
        assertThat(requester.getId()).isEqualTo(REQUESTER_ID);
        assertThat(requester.getIdentityProvider()).isEqualTo(ISSUER);
    }

    @Test
    void build_setsThePublisherAndAnIdempotenceId() {
        var command = builder().build();

        assertThat(command.getPublisher().getSystem()).isEqualTo("swiyu-trust-management");
        assertThat(command.getPublisher().getService()).isEqualTo("swiyu-trust-management-service");
        assertThat(command.getIdentity().getIdempotenceId()).isNotBlank();
    }

    @Test
    void build_whenThePartnerIsMissing_thenRejected() {
        assertThatThrownBy(() ->
            TiHardDeleteBusinessPartnerCommandBuilder.create().requester(REQUESTER_ID, ISSUER).build()
        ).isInstanceOf(AvroMessageBuilderException.class);
    }

    @Test
    void build_whenTheRequesterIsMissing_thenRejected() {
        assertThatThrownBy(() ->
            TiHardDeleteBusinessPartnerCommandBuilder.create().businessPartnerId(PARTNER_ID).build()
        ).isInstanceOf(AvroMessageBuilderException.class);
    }

    private static TiHardDeleteBusinessPartnerCommandBuilder builder() {
        return TiHardDeleteBusinessPartnerCommandBuilder.create()
            .businessPartnerId(PARTNER_ID)
            .requester(REQUESTER_ID, ISSUER);
    }
}
