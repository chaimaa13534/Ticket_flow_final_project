package com.ticketflow.customerservice.exceptions;

import java.time.Instant;

/**
 * Format d'erreur JSON uniforme pour TOUTES les erreurs de l'API.
 *
 * Référence CdC §7.2 : "erreurs métier structurées" + §6.1 : "corrélation".
 *
 * Exemple de réponse 404 :
 * {
 *   "timestamp": "2026-11-01T12:34:56Z",
 *   "status": 404,
 *   "error": "Not Found",
 *   "code": "CUSTOMER_NOT_FOUND",
 *   "message": "Customer 42 not found",
 *   "path": "/api/v1/customers/42",
 *   "correlationId": "550e8400-e29b-41d4-a716-446655440000"
 * }
 */
public record ApiError(
        Instant timestamp,
        int status,
        String error,
        String code,
        String message,
        String path,
        String correlationId
) {}