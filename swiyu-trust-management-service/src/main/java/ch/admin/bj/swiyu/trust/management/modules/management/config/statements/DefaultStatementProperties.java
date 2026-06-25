package ch.admin.bj.swiyu.trust.management.modules.management.config.statements;

import jakarta.validation.Valid;
import java.time.Period;
import org.springframework.boot.context.properties.ConfigurationProperties;

@Valid
@ConfigurationProperties(prefix = "app.statement-defaults")
public record DefaultStatementProperties(
    @Valid Period timeToLive,
    @Valid StatuslistProperties statuslist,
    @Valid NonComplianceTrustListStatementProperties nonComplianceTrustListStatement,
    @Valid ProtectedIssuanceTrustListStatementProperties protectedIssuanceTrustListStatement
) {
    /**
     * @param timeToLive Period after which a statuslist expires
     * @param size       Size of a statuslist
     */
    public record StatuslistProperties(@Valid Period timeToLive, int size) {}

    /**
     * @param timeToLive Period after which a ncTLS expires
     */
    public record NonComplianceTrustListStatementProperties(@Valid Period timeToLive) {}

    /**
     * @param timeToLive Period after which a ncTLS expires
     */
    public record ProtectedIssuanceTrustListStatementProperties(@Valid Period timeToLive) {}
}
