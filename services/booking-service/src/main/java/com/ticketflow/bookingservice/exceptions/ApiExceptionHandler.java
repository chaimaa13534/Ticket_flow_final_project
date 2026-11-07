package com.ticketflow.bookingservice.exceptions;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.ConstraintViolationException;
import java.util.NoSuchElementException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.orm.ObjectOptimisticLockingFailureException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

@RestControllerAdvice
public class ApiExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(ApiExceptionHandler.class);

    private String path(HttpServletRequest req) {
        return req != null ? req.getMethod() + " " + req.getRequestURI() : "unknown";
    }

    @ExceptionHandler({ResourceNotFoundException.class, NoSuchElementException.class})
    public ResponseEntity<ApiError> handleNotFound(RuntimeException ex, HttpServletRequest req) {
        ApiError err = ApiError.of(404, "Not Found", "RESOURCE_NOT_FOUND", ex.getMessage(), path(req));
        log.warn("[{}] 404 - {} - {}", err.correlationId(), err.path(), ex.getMessage());
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(err);
    }

    @ExceptionHandler(BookingRuleException.class)
    public ResponseEntity<ApiError> handleBookingRule(BookingRuleException ex, HttpServletRequest req) {
        ApiError err = ApiError.of(409, "Conflict", "BOOKING_RULE_VIOLATION", ex.getMessage(), path(req));
        log.warn("[{}] 409 - {} - {}", err.correlationId(), err.path(), ex.getMessage());
        return ResponseEntity.status(HttpStatus.CONFLICT).body(err);
    }

    @ExceptionHandler(ObjectOptimisticLockingFailureException.class)
    public ResponseEntity<ApiError> handleOptimisticLock(ObjectOptimisticLockingFailureException ex, HttpServletRequest req) {
        ApiError err = ApiError.of(409, "Conflict", "CONCURRENT_MODIFICATION",
                "Resource was modified by another transaction. Please retry.", path(req));
        log.warn("[{}] 409 - {} - Optimistic lock failure", err.correlationId(), err.path());
        return ResponseEntity.status(HttpStatus.CONFLICT).body(err);
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiError> handleValidation(MethodArgumentNotValidException ex, HttpServletRequest req) {
        String message = ex.getBindingResult().getFieldErrors().stream()
                .map(fe -> fe.getField() + " " + fe.getDefaultMessage())
                .findFirst()
                .orElse("Validation failed");
        ApiError err = ApiError.of(400, "Bad Request", "VALIDATION_ERROR", message, path(req));
        log.warn("[{}] 400 - {} - {}", err.correlationId(), err.path(), message);
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(err);
    }

    @ExceptionHandler(ConstraintViolationException.class)
    public ResponseEntity<ApiError> handleConstraint(ConstraintViolationException ex, HttpServletRequest req) {
        ApiError err = ApiError.of(400, "Bad Request", "VALIDATION_ERROR", ex.getMessage(), path(req));
        log.warn("[{}] 400 - {} - {}", err.correlationId(), err.path(), ex.getMessage());
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(err);
    }

    @ExceptionHandler({HttpMessageNotReadableException.class, MethodArgumentTypeMismatchException.class})
    public ResponseEntity<ApiError> handleMalformed(Exception ex, HttpServletRequest req) {
        ApiError err = ApiError.of(400, "Bad Request", "MALFORMED_REQUEST",
                "Request body is malformed or parameter type is invalid", path(req));
        log.warn("[{}] 400 - {} - Malformed", err.correlationId(), err.path());
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(err);
    }

    @ExceptionHandler(DataIntegrityViolationException.class)
    public ResponseEntity<ApiError> handleDataIntegrity(DataIntegrityViolationException ex, HttpServletRequest req) {
        ApiError err = ApiError.of(409, "Conflict", "DATA_INTEGRITY_VIOLATION",
                "Operation conflicts with existing data", path(req));
        log.warn("[{}] 409 - {} - Data integrity", err.correlationId(), err.path());
        return ResponseEntity.status(HttpStatus.CONFLICT).body(err);
    }

    @ExceptionHandler(IllegalStateException.class)
    public ResponseEntity<ApiError> handleIllegalState(IllegalStateException ex, HttpServletRequest req) {
        ApiError err = ApiError.of(409, "Conflict", "INVALID_STATE_TRANSITION", ex.getMessage(), path(req));
        log.warn("[{}] 409 - {} - {}", err.correlationId(), err.path(), ex.getMessage());
        return ResponseEntity.status(HttpStatus.CONFLICT).body(err);
    }

    @ExceptionHandler(UpstreamUnavailableException.class)
    public ResponseEntity<ApiError> handleUpstream(UpstreamUnavailableException ex, HttpServletRequest req) {
        ApiError err = ApiError.of(503, "Service Unavailable", "UPSTREAM_UNAVAILABLE",
                "Upstream service '" + ex.getUpstreamService() + "' is temporarily unavailable. Please retry.",
                path(req));
        log.warn("[{}] 503 - {} - {}", err.correlationId(), err.path(), ex.getMessage());
        return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE).body(err);
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiError> handleGeneric(Exception ex, HttpServletRequest req) {
        String cid = java.util.UUID.randomUUID().toString();
        ApiError err = ApiError.of(500, "Internal Server Error", "INTERNAL_ERROR",
                "An unexpected error occurred. Contact support with correlationId " + cid, path(req));
        log.error("[{}] 500 - {} - Unexpected", cid, err.path(), ex);
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(err);
    }
}