package com.ticketflow.bookingservice.clients;

import java.math.BigDecimal;
import java.time.Instant;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

/**
 * Client REST vers event-service (OpenFeign, résolution Eureka via lb://).
 *
 * Récupère les informations d'un événement et de ses paliers de tarif.
 * La résilience (circuit breaker, retry) est configurée dans application.yml via Resilience4J.
 */
@FeignClient(name = "event-service", url = "${ticketflow.clients.event-service.url:}")
public interface EventClient {

    @GetMapping("/api/v1/events/{id}")
    EventView getEvent(@PathVariable("id") Long id);

    @GetMapping("/api/v1/ticket-tiers/by-event/{eventId}")
    java.util.List<TierView> getTiers(@PathVariable("eventId") Long eventId);

    /** DTO de lecture d'un événement (uniquement les champs nécessaires au booking). */
    record EventView(
            Long id,
            String title,
            Long organizerId,
            Instant startsAt,
            BigDecimal basePrice,
            String currency,
            int capacity,
            String status
    ) {}

    /** DTO de lecture d'un palier de tarif. */
    record TierView(
            Long id,
            String name,
            BigDecimal price,
            int quantityAvailable
    ) {}
}
