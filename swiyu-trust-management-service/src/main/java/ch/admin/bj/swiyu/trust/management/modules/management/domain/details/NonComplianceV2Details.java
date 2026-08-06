package ch.admin.bj.swiyu.trust.management.modules.management.domain.details;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.ToString;

/**
 * Represents the details of a trust statement of type TrustStatementIdentityV2.
 * See <a href="https://confluence.bit.admin.ch/spaces/EIDTEAM/pages/1374138607/Trust+Protocol+2.0#TrustProtocol2.0-IdentityTrustStatement(idTS)Identity-Trust-Statement">spec</a>
 */
@ToString(callSuper = true)
@EqualsAndHashCode(callSuper = true)
@Getter
public final class NonComplianceV2Details extends TrustStatementDetails {

    private List<NonCompliantActor> nonCompliantActors;

    NonComplianceV2Details() {
        super(TrustStatementPartnerLinkType.TRUST_LIST_STATEMENT_NON_COMPLIANCE_V2);
    }

    public NonComplianceV2Details(List<NonCompliantActor> nonCompliantActors) {
        this();
        this.nonCompliantActors = nonCompliantActors;
    }

    public record NonCompliantActor(String actor, Instant flaggedAt, Map<String, String> reason) {}
}
