package ch.admin.bj.swiyu.trust.management.test;

import lombok.experimental.UtilityClass;
import org.springframework.test.context.transaction.TestTransaction;

@UtilityClass
public class TestTransactionSupport {

    public static void commit() {
        TestTransaction.flagForCommit();
        TestTransaction.end();
    }
}
