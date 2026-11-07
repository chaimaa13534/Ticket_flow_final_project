package com.ticketflow.customerservice.exceptions;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.ConstraintViolationException;
import java.time.Instant;
import java.util.NoSuchElementException;
import java.util.UUID;
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
 * Intercepte TOUTES les exceptions levées par les controllers et renvoie un format JSON
 * uniforme {@link ApiError}. CdC §7.2 : "erreurs métier structurées" et "détails d'erreur
 * sans stack trace ni secret".
 *
 * Sans trace, sans secret — juste : code métier + message + path + correlationId.
 */
@RestControllerAdvice
public class ApiExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(ApiExceptionHandler.class);

    private String correlationId() {
        return UUID.randomUUID().toString();
    }

    private String path(HttpServletRequest req) {
        return req != null ? req.getMethod() + " " + req.getRequestURI() : "unknown";
    }

    @ExceptionHandler({ResourceNotFoundException.class, NoSuchElementException.class})
    public ResponseEntity<ApiError> handleNotFound(RuntimeException ex, HttpServletRequest req) {
        String cid = correlationId();
        log.warn("[{}] 404 - {} - {}", cid, path(req), ex.getMessage());
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(new ApiError(
                Instant.now(), 404, "Not Found", "RESOURCE_NOT_FOUND",
                ex.getMessage(), path(req), cid
        ));
    }

    @ExceptionHandler(DuplicateEmailException.class)
    public ResponseEntity<ApiError> handleDuplicateEmail(DuplicateEmailException ex, HttpServletRequest req) {
        String cid = correlationId();
        log.warn("[{}] 409 - {} - {}", cid, path(req), ex.getMessage());
        return ResponseEntity.status(HttpStatus.CONFLICT).body(new ApiError(
                Instant.now(), 409, "Conflict", "DUPLICATE_EMAIL",
                ex.getMessage(), path(req), cid
        ));
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiError> handleValidation(MethodArgumentNotValidException ex, HttpServletRequest req) {
        String cid = correlationId();
        String message = ex.getBindingResult().getFieldErrors().stream()
                .map(fe -> fe.getField() + " " + fe.getDefaultMessage())
                .findFirst()
                .orElse("Validation failed");
        log.warn("[{}] 400 - {} - {}", cid, path(req), message);
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(new ApiError(
                Instant.now(), 400, "Bad Request", "VALIDATION_ERROR",
                message, path(req), cid
        ));
    }

    @ExceptionHandler(ConstraintViolationException.class)
    public ResponseEntity<ApiError> handleConstraint(ConstraintViolationException ex, HttpServletRequest req) {
        String cid = correlationId();
        log.warn("[{}] 400 - {} - {}", cid, path(req), ex.getMessage());
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(new ApiError(
                Instant.now(), 400, "Bad Request", "VALIDATION_ERROR",
                ex.getMessage(), path(req), cid
        ));
    }

    @ExceptionHandler({HttpMessageNotReadableException.class, MethodArgumentTypeMismatchException.class})
    public ResponseEntity<ApiError> handleMalformed(Exception ex, HttpServletRequest req) {
        String cid = correlationId();
        log.warn("[{}] 400 - {} - Malformed request", cid, path(req));
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(new ApiError(
                Instant.now(), 400, "Bad Request", "MALFORMED_REQUEST",
                "Request body is malformed or parameter type is invalid",
                path(req), cid
        ));
    }

    @ExceptionHandler(DataIntegrityViolationException.class)
    public ResponseEntity<ApiError> handleDataIntegrity(DataIntegrityViolationException ex, HttpServletRequest req) {
        String cid = correlationId();
        log.warn("[{}] 409 - {} - Data integrity violation", cid, path(req));
        return ResponseEntity.status(HttpStatus.CONFLICT).body(new ApiError(
                Instant.now(), 409, "Conflict", "DATA_INTEGRITY_VIOLATION",
                "Operation conflicts with existing data", path(req), cid
        ));
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiError> handleGeneric(Exception ex, HttpServletRequest req) {
        String cid = correlationId();
        log.error("[{}] 500 - {} - Unexpected error", cid, path(req), ex);
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(new ApiError(
                Instant.now(), 500, "Internal Server Error", "INTERNAL_ERROR",
                "An unexpected error occurred. Contact support with correlationId " + cid,
                path(req), cid
        ));
    }
}