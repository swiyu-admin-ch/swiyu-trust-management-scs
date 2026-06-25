package ch.admin.bj.swiyu.trust.management.modules.common.exception;

public class IssuanceException extends RuntimeException {

    public IssuanceException(String message) {
        super(message);
    }

    public IssuanceException(String message, Throwable cause) {
        super(message, cause);
    }
}
