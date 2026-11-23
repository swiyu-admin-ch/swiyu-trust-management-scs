package ch.admin.bj.swiyu.trust.management.modules.common.async;

import lombok.experimental.UtilityClass;

@UtilityClass
public class Lock {

    public static final String DEACTIVATE_EXPIRED_BUSINESS_PARTNER_IDENTITIES =
        "DEACTIVATE_EXPIRED_BUSINESS_PARTNER_IDENTITIES";
    public static final String NON_COMPLIANCE_TRUST_LIST_PUBLISHING = "NON_COMPLIANCE_TRUST_LIST_PUBLISHING";
    public static final String PROTECTED_ISSUANCE_TRUST_LIST_PUBLISHING = "PROTECTED_ISSUANCE_TRUST_LIST_PUBLISHING";
    public static final String STATUS_LIST_PUBLISHING = "STATUS_LIST_PUBLISHING";
    public static final String STATEMENT_REFRESH = "STATEMENT_REFRESH";
    public static final String TRUST_ONBOARDING_TASK_REJECTION_ENFORCEMENT =
        "TRUST_ONBOARDING_TASK_REJECTION_ENFORCEMENT";
    public static final String HARD_DELETE_BUSINESS_PARTNER = "HARD_DELETE_BUSINESS_PARTNER";
    public static final String JAVA_MIGRATION = "JAVA_MIGRATION";
    public static final String LOST_STATEMENTS_IDENTIFICATION = "LOST_STATEMENTS_IDENTIFICATION";
    public static final String DEMO_DATA_IMPORT = "DEMO_DATA_IMPORT";
}
