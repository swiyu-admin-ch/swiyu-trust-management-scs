package ch.admin.bj.swiyu.trust.management.modules.common.exception;

public class VcSchemaPublicationFailedException extends RuntimeException {

    public VcSchemaPublicationFailedException(String message) {
        super(message);
    }

    public VcSchemaPublicationFailedException(String message, Throwable cause) {
        super(message, cause);
    }
}
