package ch.admin.bj.swiyu.trust.management.modules.common.exception;

import lombok.AllArgsConstructor;
import lombok.Getter;
import org.springframework.validation.SimpleErrors;

@AllArgsConstructor
@Getter
public class TrustStatementValidationFailedException extends RuntimeException {

    private final SimpleErrors validationIssues;
}
