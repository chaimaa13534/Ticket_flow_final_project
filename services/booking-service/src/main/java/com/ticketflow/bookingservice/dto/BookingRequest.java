package com.ticketflow.bookingservice.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import java.util.List;

/**
 * Corps de requête pour créer une réservation (CdC FR-007).
 *
 * Le {@code customerId} sera extrait du JWT à l'étape 4 (sub du rôle CLIENT).
 * Pour l'instant, il doit être fourni dans le body (transitionnel).
 *
 * Une réservation peut contenir PLUSIEURS lignes (paliers différents).
 */
public record BookingRequest(
        @NotNull @Positive Long eventId,
        @NotNull @Positive Long customerId,
        @NotNull List<@NotNull LineRequest> lines
) {
    public record LineRequest(
            @NotNull @Positive Long ticketTierId,
            @NotNull @Min(1) int quantity
    ) {}
}