package ch.admin.bj.swiyu.trust.management.modules.management.domain;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public enum TokenStatusListBit {
    VALID(0),
    REVOKE(1),
    SUSPEND(2),
    MAX(2);

    /**
     * Value as defined in Token Status List Spec
     *
     * @link <a href="https://www.ietf.org/archive/id/draft-ietf-oauth-status-list-15.html#section-7.1">Token Status List Spec</a>
     */
    private final int value;
}
