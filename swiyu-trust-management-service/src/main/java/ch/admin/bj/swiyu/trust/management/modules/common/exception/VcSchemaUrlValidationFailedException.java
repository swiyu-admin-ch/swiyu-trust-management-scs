package ch.admin.bj.swiyu.trust.management.modules.common.exception;

import lombok.AllArgsConstructor;
import lombok.Getter;

@AllArgsConstructor
@Getter
public class VcSchemaUrlValidationFailedException extends RuntimeException {

    public VcSchemaUrlValidationFailedException(String message) {
        super(message);
    }
}
