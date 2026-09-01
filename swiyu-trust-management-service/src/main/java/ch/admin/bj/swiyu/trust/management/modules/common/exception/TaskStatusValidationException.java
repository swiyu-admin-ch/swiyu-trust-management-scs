package ch.admin.bj.swiyu.trust.management.modules.common.exception;

import lombok.AllArgsConstructor;
import lombok.Getter;

@AllArgsConstructor
@Getter
public class TaskStatusValidationException extends RuntimeException {

    public TaskStatusValidationException(String message) {
        super(message);
    }

    public TaskStatusValidationException(String message, Throwable cause) {
        super(message, cause);
    }
}
