package com.ticketflow.mcptoolserver.controllers;

import com.ticketflow.mcptoolserver.clients.EventClient.EventView;
import com.ticketflow.mcptoolserver.services.McpToolService;
import java.util.List;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * API REST MCP Tool Server (CdC §7.4).
 *
 * Expose les outils allowlistés que l'AI Assistant peut appeler.
 * Le LLM n'a JAMAIS accès direct aux services métier.
 */
@RestController
@RequestMapping("/api/v1/tools")
public class McpToolController {

    private final McpToolService service;

    public McpToolController(McpToolService service) {
        this.service = service;
    }

    /**
     * Outil : search_public_events
     * GET /api/v1/tools/search_public_events?page=0&size=5
     */
    @GetMapping("/search_public_events")
    public ResponseEntity<List<EventView>> searchPublicEvents(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "5") int size) {
        return ResponseEntity.ok(service.searchPublicEvents(page, size));
    }

    /**
     * Outil : get_public_event
     * GET /api/v1/tools/get_public_event?eventId=1
     */
    @GetMapping("/get_public_event")
    public ResponseEntity<EventView> getPublicEvent(@RequestParam Long eventId) {
        return ResponseEntity.ok(service.getPublicEvent(eventId));
    }
}
