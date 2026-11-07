package com.ticketflow.eventservice.dto;

import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.Future;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;
import java.time.Instant;

/**
 * Corps de requête pour créer ou mettre à jour un événement.
 *
 * Le {@code organizerId} sera extrait du JWT à l'étape 4 (rôle ORGANIZER).
 * Pour l'instant, il doit être fourni dans le body (transitionnel).
 */
public record EventRequest(
        @NotBlank @Size(max = 150) String title,
        @Size(max = 2000) String description,
        @NotNull @Positive Long venueId,
        @NotNull @Positive Long categoryId,
        @NotNull @Positive Long organizerId,
        @NotNull @Future Instant startsAt,
        Instant endsAt,
        @NotNull @PositiveOrZero @Digits(integer = 8, fraction = 2) BigDecimal basePrice,
        @Positive int capacity
) {}
