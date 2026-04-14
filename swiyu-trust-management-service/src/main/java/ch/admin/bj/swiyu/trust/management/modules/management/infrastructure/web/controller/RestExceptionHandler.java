package ch.admin.bj.swiyu.trust.management.modules.management.infrastructure.web.controller;

import ch.admin.bj.swiyu.trust.management.modules.common.exception.*;
import jakarta.servlet.http.*;
import java.util.stream.*;
import lombok.*;
import lombok.extern.slf4j.*;
import org.springframework.http.*;
import org.springframework.security.authorization.*;
import org.springframework.validation.*;
import org.springframework.web.bind.*;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.context.request.*;
import org.springframework.web.servlet.mvc.method.annotation.*;

/**
 * Default Exception handler for exceptions that are common for all controllers.
 */
@RestControllerAdvice
@Slf4j
public class RestExceptionHandler extends ResponseEntityExceptionHandler {

    @ExceptionHandler(ResourceNotFoundException.class)
    @ResponseStatus(HttpStatus.NOT_FOUND)
    public void handleResourceNotFound(Exception e, HttpServletRequest request) {
        log.debug("Resource not found for url {}", request.getRequestURL(), e);
    }

    @ExceptionHandler(ExternalSystemProxyException.class)
    public ResponseEntity<String> handleResourceNotFound(ExternalSystemProxyException e, HttpServletRequest request) {
        log.debug("External system did respond with error {}", request.getRequestURL(), e);
        return new ResponseEntity<>(
            e.getHttpClientErrorException().getResponseBodyAsString(),
            e.getHttpClientErrorException().getStatusCode()
        );
    }

    @ExceptionHandler(TrustStatementValidationFailedException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public SimpleErrors handleTrustStatementValidationFailedException(TrustStatementValidationFailedException e) {
        return e.getValidationIssues();
    }

    @ExceptionHandler(AuthorizationDeniedException.class)
    @ResponseStatus(HttpStatus.UNAUTHORIZED)
    public void handleUnauthorized(final Exception exception, HttpServletRequest request) {
        log.debug("Access to {} prevented since unauthorized", request.getRequestURL(), exception);
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<Object> handleIllegalArgument(final Exception exception, HttpServletRequest request) {
        log.debug("Received bad request for URL {}. Details: {}", request.getRequestURL(), exception.getMessage());
        return new ResponseEntity<>(exception.getMessage(), HttpStatus.BAD_REQUEST);
    }

    @Override
    protected ResponseEntity<Object> handleMethodArgumentNotValid(
        MethodArgumentNotValidException ex,
        @NonNull HttpHeaders headers,
        @NonNull HttpStatusCode status,
        @NonNull WebRequest request
    ) {
        var errors = ex
            .getBindingResult()
            .getFieldErrors()
            .stream()
            .map(error -> String.format("%s: %s", error.getField(), error.getDefaultMessage()))
            .sorted()
            .collect(Collectors.joining(", "));

        log.debug("Received bad request. Details: {}", errors);

        return new ResponseEntity<>(errors, HttpStatus.UNPROCESSABLE_ENTITY);
    }

    @ExceptionHandler(Exception.class)
    @ResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR)
    public void handleUnexpectedErrors(final Exception exception, HttpServletRequest request) {
        log.error("Detected unhandled exception for URL {}", request.getRequestURL(), exception);
    }
}
