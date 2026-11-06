package com.ticketing.booking.api;

import com.ticketing.booking.catalog.CatalogUnavailableException;
import com.ticketing.booking.catalog.EventNotFoundException;
import com.ticketing.booking.hold.AlreadyHoldingException;
import com.ticketing.booking.hold.HoldNotFoundException;
import com.ticketing.booking.hold.InvalidHoldRequestException;
import com.ticketing.booking.hold.SeatsUnavailableException;
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

/** Same Problem Details format as event-service (ADR 0005). */
@RestControllerAdvice
public class GlobalExceptionHandler extends ResponseEntityExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    // 409 + the exact seats that were taken, so the UI can mark them and keep the rest selected.
    @ExceptionHandler(SeatsUnavailableException.class)
    ProblemDetail handleSeatsUnavailable(SeatsUnavailableException ex) {
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(HttpStatus.CONFLICT,
                "Some of the selected seats were just taken by someone else.");
        problem.setProperty("unavailableSeatIds", ex.getSeatIds());
        return problem;
    }

    @ExceptionHandler(AlreadyHoldingException.class)
    ProblemDetail handleAlreadyHolding(AlreadyHoldingException ex) {
        return ProblemDetail.forStatusAndDetail(HttpStatus.CONFLICT, ex.getMessage());
    }

    @ExceptionHandler({HoldNotFoundException.class, EventNotFoundException.class})
    ProblemDetail handleNotFound(RuntimeException ex) {
        return ProblemDetail.forStatusAndDetail(HttpStatus.NOT_FOUND, ex.getMessage());
    }

    @ExceptionHandler({InvalidHoldRequestException.class, UserIds.InvalidUserIdException.class})
    ProblemDetail handleBadRequest(RuntimeException ex) {
        return ProblemDetail.forStatusAndDetail(HttpStatus.BAD_REQUEST, ex.getMessage());
    }

    // A dependency is down: 503 tells clients "try again later", unlike a generic 500.
    @ExceptionHandler(CatalogUnavailableException.class)
    ProblemDetail handleCatalogUnavailable(CatalogUnavailableException ex) {
        log.warn("event-service call failed: {}", ex.getCause() == null ? ex.getMessage() : ex.getCause().toString());
        return ProblemDetail.forStatusAndDetail(HttpStatus.SERVICE_UNAVAILABLE,
                "Seat information is temporarily unavailable. Please try again.");
    }

    @ExceptionHandler(Exception.class)
    ProblemDetail handleUnexpected(Exception ex) {
        log.error("Unhandled exception", ex);
        return ProblemDetail.forStatusAndDetail(HttpStatus.INTERNAL_SERVER_ERROR, "An unexpected error occurred.");
    }

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
}
