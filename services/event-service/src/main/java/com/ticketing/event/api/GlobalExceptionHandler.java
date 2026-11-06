package com.ticketing.event.api;

import com.ticketing.event.domain.InvalidEventStateException;
import com.ticketing.event.service.EventNotFoundException;
import com.ticketing.event.service.VenueNotFoundException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.context.request.WebRequest;
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Turns every error into the same JSON shape: RFC 9457 "Problem Details"
 * (content type application/problem+json):
 *
 * <pre>
 * { "type": "about:blank", "title": "Not Found", "status": 404,
 *   "detail": "Event 42 not found", "instance": "/events/42" }
 * </pre>
 *
 * The parent class ResponseEntityExceptionHandler already converts Spring MVC's own
 * errors (malformed JSON, wrong parameter types, unknown URLs, ResponseStatusException)
 * into Problem Details. The methods below add our domain exceptions and a safe fallback.
 */
@RestControllerAdvice
public class GlobalExceptionHandler extends ResponseEntityExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    @ExceptionHandler(EventNotFoundException.class)
    ProblemDetail handleEventNotFound(EventNotFoundException ex) {
        return problem(HttpStatus.NOT_FOUND, ex.getMessage());
    }

    @ExceptionHandler(VenueNotFoundException.class)
    ProblemDetail handleVenueNotFound(VenueNotFoundException ex) {
        return problem(HttpStatus.BAD_REQUEST, ex.getMessage());
    }

    @ExceptionHandler(InvalidEventStateException.class)
    ProblemDetail handleInvalidState(InvalidEventStateException ex) {
        return problem(HttpStatus.CONFLICT, ex.getMessage());
    }

    /**
     * Anything we didn't expect. Log the full stack trace on the server, but never send it
     * to the client: internals (SQL, class names) are both useless and risky to expose.
     */
    @ExceptionHandler(Exception.class)
    ProblemDetail handleUnexpected(Exception ex) {
        log.error("Unhandled exception", ex);
        return problem(HttpStatus.INTERNAL_SERVER_ERROR, "An unexpected error occurred.");
    }

    /**
     * Bean Validation failures on @Valid request bodies. Adds an "errors" object with one
     * message per invalid field, so a form can show each message next to its field:
     * { ..., "errors": { "title": "must not be blank", "startTime": "must be a future date" } }
     */
    @Override
    protected ResponseEntity<Object> handleMethodArgumentNotValid(
            MethodArgumentNotValidException ex, HttpHeaders headers, HttpStatusCode status, WebRequest request) {

        Map<String, String> errors = new LinkedHashMap<>();
        for (FieldError fieldError : ex.getBindingResult().getFieldErrors()) {
            errors.putIfAbsent(fieldError.getField(), fieldError.getDefaultMessage());
        }

        ProblemDetail body = ex.getBody();
        body.setDetail("Validation failed for " + errors.size() + " field(s).");
        body.setProperty("errors", errors);
        return handleExceptionInternal(ex, body, headers, status, request);
    }

    private static ProblemDetail problem(HttpStatus status, String detail) {
        return ProblemDetail.forStatusAndDetail(status, detail);
    }
}
