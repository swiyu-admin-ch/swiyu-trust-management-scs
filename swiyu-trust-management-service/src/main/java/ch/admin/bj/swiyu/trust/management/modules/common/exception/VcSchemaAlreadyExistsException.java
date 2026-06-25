package ch.admin.bj.swiyu.trust.management.modules.common.exception;

import lombok.AllArgsConstructor;
import lombok.Getter;

@AllArgsConstructor
@Getter
public class VcSchemaAlreadyExistsException extends RuntimeException {

    public VcSchemaAlreadyExistsException(String message) {
        super(message);
    }
}
