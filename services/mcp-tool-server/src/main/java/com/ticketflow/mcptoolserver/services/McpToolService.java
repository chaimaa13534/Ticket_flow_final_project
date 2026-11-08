package com.ticketflow.mcptoolserver.services;

import com.ticketflow.mcptoolserver.clients.EventClient;
import com.ticketflow.mcptoolserver.clients.EventClient.EventView;
import com.ticketflow.mcptoolserver.clients.EventClient.PageResponse;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

@Service
public class McpToolService {

    private static final Logger log = LoggerFactory.getLogger(McpToolService.class);

    private final EventClient eventClient;

    public McpToolService(EventClient eventClient) {
        this.eventClient = eventClient;
    }

    public List<EventView> searchPublicEvents(int page, int size) {
        log.info("🔧 MCP Tool: search_public_events(page={}, size={})", page, size);
        PageResponse response = eventClient.searchEvents(page, size);
        if (response == null || response.content() == null) {
            return List.of();
        }
        return response.content();
    }

    public EventView getPublicEvent(Long eventId) {
        log.info("🔧 MCP Tool: get_public_event(id={})", eventId);
        return eventClient.getEvent(eventId);
    }
}