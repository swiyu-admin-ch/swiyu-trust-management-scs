package ch.admin.bj.swiyu.trust.management.modules.management.domain.issuer;

/**
 * Internal convenience enum to represent the status of a Verifiable Credential (VC).
 */
public enum IssuerVcStatus {
    UNKNOWN,
    VALID,
    INVALID;

    public boolean isInvalid() {
        return INVALID.equals(this);
    }
}
