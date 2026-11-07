package com.ticketflow.paymentservice.exceptions;

import jakarta.servlet.http.HttpServletRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
public class ApiExceptionHandler {
    private static final Logger log = LoggerFactory.getLogger(ApiExceptionHandler.class);

    private String path(HttpServletRequest req) {
        return req != null ? req.getMethod() + " " + req.getRequestURI() : "unknown";
    }

    @ExceptionHandler(ResourceNotFoundException.class)
    public ResponseEntity<ApiError> handleNotFound(RuntimeException ex, HttpServletRequest req) {
        ApiError err = ApiError.of(404, "Not Found", "RESOURCE_NOT_FOUND", ex.getMessage(), path(req));
        log.warn("[{}] 404 - {} - {}", err.correlationId(), err.path(), ex.getMessage());
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(err);
    }

    @ExceptionHandler(IllegalStateException.class)
    public ResponseEntity<ApiError> handleIllegalState(IllegalStateException ex, HttpServletRequest req) {
        ApiError err = ApiError.of(409, "Conflict", "INVALID_STATE_TRANSITION", ex.getMessage(), path(req));
        log.warn("[{}] 409 - {} - {}", err.correlationId(), err.path(), ex.getMessage());
        return ResponseEntity.status(HttpStatus.CONFLICT).body(err);
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
