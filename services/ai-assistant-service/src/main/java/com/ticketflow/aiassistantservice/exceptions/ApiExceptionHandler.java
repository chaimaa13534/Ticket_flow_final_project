package com.ticketflow.aiassistantservice.exceptions;

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

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiError> handleGeneric(Exception ex, HttpServletRequest req) {
        String cid = java.util.UUID.randomUUID().toString();
        ApiError err = ApiError.of(500, "Internal Server Error", "INTERNAL_ERROR",
                "An unexpected error occurred. Contact support with correlationId " + cid, path(req));
        log.error("[{}] 500 - {} - Unexpected", cid, err.path(), ex);
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(err);
    }
}
