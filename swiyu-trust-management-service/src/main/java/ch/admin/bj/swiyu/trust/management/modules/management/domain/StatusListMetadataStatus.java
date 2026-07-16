package ch.admin.bj.swiyu.trust.management.modules.management.domain;

import ch.admin.bj.swiyu.trust.management.modules.jobs.service.StatementRefreshJob;

public enum StatusListMetadataStatus {
    /**
     * A statuslist is considered ACTIVE if there are free index entries to be assigned to a statement.
     * <p>
     * An ACTIVE statuslist gets an automatic refresh of the statuslist token with {@link StatementRefreshJob#refreshStatusLists()}.
     */
    ACTIVE,
    /**
     * A statuslist is considered FULL if all available index entries are accosted with a statement.
     * No new index can be assigned from this statuslist.
     * <p>
     * A FULL statuslist gets an automatic refresh of the statuslist token with {@link StatementRefreshJob#refreshStatusLists()}.
     */
    FULL,
    /**
     * A statuslist is considered DEACTIVATED if we do no longer have control over it.
     * (Example: We do no longer have access to the issuer DID to manage this statuslist)
     * A DEACTIVATED statuslist should only contain REVOKED entries and should be indefinitely valid,
     * so should not contain the "exp" claim on the statuslist token.
     * <p>
     * DO NOT assign any new statements to DEACTIVATED statuslists.
     * <p>
     * A DEACTIVATED statuslist is not considered for the automatic refresh of the statuslist tokens.
     */
    DEACTIVATED,
}
