package ch.admin.bj.swiyu.trust.management.modules.common.persistence;

import lombok.experimental.*;

/**
 * Holds constants of all registered transaction manager names;
 */
@UtilityClass
public class TransactionManagerNames {

    /**
     * Registered name of the transaction manager for accessing the management database.
     */
    public static final String MANAGEMENT_TRANSACTION_MANAGER = "managementTransactionManager";
    /**
     * Registered name of the transaction manager for accessing the public trust registry database.
     */
    public static final String REGISTRY_TRANSACTION_MANAGER = "registryTransactionManager";
}
