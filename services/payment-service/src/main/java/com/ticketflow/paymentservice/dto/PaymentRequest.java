package com.ticketflow.paymentservice.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

/**
 * Corps de requête pour SIMULER un paiement (FR-009).
 * En mode simulé, on fournit juste l'ID de la réservation à payer.
 */
public record PaymentRequest(
        @NotNull @Positive Long bookingId,
        @NotNull @Positive Long customerId
) {}
