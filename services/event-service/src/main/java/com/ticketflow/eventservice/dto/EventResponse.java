package com.ticketflow.eventservice.dto;

import com.ticketflow.eventservice.entities.Event;
import com.ticketflow.eventservice.entities.EventStatus;
import java.math.BigDecimal;
import java.time.Instant;

/**
 * Réponse détaillée d'un événement (FR-003 : fiche événement).
 *
 * IMPORTANT : la conversion Event -> Response prend l'ID du venue et de la category,
 * PAS les objets complets. Les détails sont récupérés séparément via /venues/{id} et
 * /categories/{id} si nécessaire (évite N+1 et payload lourd).
 */
public record EventResponse(
        Long id,
        String title,
        String description,
        Long venueId,
        String venueName,
        String venueCity,
        Long categoryId,
        String categoryName,
        String categorySlug,
        Long organizerId,
        Instant startsAt,
        Instant endsAt,
        BigDecimal basePrice,
        String currency,
        int capacity,
        EventStatus status,
        Instant publishedAt,
        Instant createdAt
) {
    public static EventResponse from(Event e) {
        return new EventResponse(
                e.getId(),
                e.getTitle(),
                e.getDescription(),
                e.getVenue() != null ? e.getVenue().getId() : null,
                e.getVenue() != null ? e.getVenue().getName() : null,
                e.getVenue() != null ? e.getVenue().getCity() : null,
                e.getCategory() != null ? e.getCategory().getId() : null,
                e.getCategory() != null ? e.getCategory().getName() : null,
                e.getCategory() != null ? e.getCategory().getSlug() : null,
                e.getOrganizerId(),
                e.getStartsAt(),
                e.getEndsAt(),
                e.getBasePrice(),
                e.getCurrency(),
                e.getCapacity(),
                e.getStatus(),
                e.getPublishedAt(),
                e.getCreatedAt()
        );
    }
}