package com.ticketflow.mcptoolserver.clients;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

@FeignClient(name = "event-service")
public interface EventClient {

    @GetMapping("/api/v1/events")
    PageResponse searchEvents(
            @RequestParam(value = "page", defaultValue = "0") int page,
            @RequestParam(value = "size", defaultValue = "10") int size
    );

    @GetMapping("/api/v1/events/{id}")
    EventView getEvent(@PathVariable("id") Long id);

    /** DTO de lecture d'un événement. */
    record EventView(
            Long id,
            String title,
            String description,
            Long venueId,
            String venueName,
            String venueCity,
            Long categoryId,
            String categoryName,
            Instant startsAt,
            BigDecimal basePrice,
            String status
    ) {}

    /** DTO pour représenter la pagination de Spring Data */
    record PageResponse(
            List<EventView> content,
            int totalElements,
            int totalPages,
            int number
    ) {}
}