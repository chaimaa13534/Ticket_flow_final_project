package com.ticketflow.aiassistantservice.exceptions;

import java.time.Instant;
import java.util.UUID;

public record ApiError(
        Instant timestamp,
        int status,
        String error,
        String code,
        String message,
        String path,
        String correlationId
) {
    public static ApiError of(int status, String error, String code, String message, String path) {
        return new ApiError(Instant.now(), status, error, code, message, path, UUID.randomUUID().toString());
    }
}