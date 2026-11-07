package com.ticketflow.eventservice.exceptions;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.ConstraintViolationException;
import java.util.NoSuchElementException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

/**
 * Intercepte TOUTES les exceptions des controllers et renvoie un format JSON uniforme
 * {@link ApiError}. CdC §7.2 : "erreurs métier structurées" et "détails sans stack trace ni secret".
 */
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

    @ExceptionHandler(InvalidEventStateException.class)
    public ResponseEntity<ApiError> handleInvalidState(InvalidEventStateException ex, HttpServletRequest req) {
        ApiError err = ApiError.of(409, "Conflict", "INVALID_EVENT_STATE", ex.getMessage(), path(req));
        log.warn("[{}] 409 - {} - {}", err.correlationId(), err.path(), ex.getMessage());
        return ResponseEntity.status(HttpStatus.CONFLICT).body(err);
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<ApiError> handleIllegalArgument(IllegalArgumentException ex, HttpServletRequest req) {
        ApiError err = ApiError.of(400, "Bad Request", "INVALID_ARGUMENT", ex.getMessage(), path(req));
        log.warn("[{}] 400 - {} - {}", err.correlationId(), err.path(), ex.getMessage());
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(err);
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
        log.warn("[{}] 400 - {} - Malformed request", err.correlationId(), err.path());
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(err);
    }

    @ExceptionHandler(DataIntegrityViolationException.class)
    public ResponseEntity<ApiError> handleDataIntegrity(DataIntegrityViolationException ex, HttpServletRequest req) {
        ApiError err = ApiError.of(409, "Conflict", "DATA_INTEGRITY_VIOLATION",
                "Operation conflicts with existing data", path(req));
        log.warn("[{}] 409 - {} - Data integrity violation", err.correlationId(), err.path());
        return ResponseEntity.status(HttpStatus.CONFLICT).body(err);
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiError> handleGeneric(Exception ex, HttpServletRequest req) {
        String cid = java.util.UUID.randomUUID().toString();
        ApiError err = ApiError.of(500, "Internal Server Error", "INTERNAL_ERROR",
                "An unexpected error occurred. Contact support with correlationId " + cid,
                path(req));
        log.error("[{}] 500 - {} - Unexpected error", cid, err.path(), ex);
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(err);
    }
}
