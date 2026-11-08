package com.ticketflow.aiassistantservice.clients;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import java.util.List;

@FeignClient(name = "mcp-tool-server")
public interface McpClient {

    @GetMapping("/api/v1/tools/search_public_events")
    List<EventView> searchPublicEvents(
            @RequestParam(value = "page", defaultValue = "0") int page,
            @RequestParam(value = "size", defaultValue = "5") int size
    );

    @GetMapping("/api/v1/tools/get_public_event")
    EventView getPublicEvent(@RequestParam("eventId") Long eventId);

    record EventView(
            Long id,
            String title,
            String description,
            String venueName,
            String venueCity,
            java.time.Instant startsAt,
            java.math.BigDecimal basePrice,
            String status
    ) {}
}
