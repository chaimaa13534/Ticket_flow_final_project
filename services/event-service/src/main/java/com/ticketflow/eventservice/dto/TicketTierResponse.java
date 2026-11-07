package com.ticketflow.eventservice.dto;

import com.ticketflow.eventservice.entities.TicketTier;
import java.math.BigDecimal;

public record TicketTierResponse(
        Long id,
        Long eventId,
        String name,
        BigDecimal price,
        int quantityAvailable,
        long version
) {
    public static TicketTierResponse from(TicketTier t) {
        return new TicketTierResponse(t.getId(), t.getEvent().getId(), t.getName(),
                t.getPrice(), t.getQuantityAvailable(),
                t.getVersion() != null ? t.getVersion() : 0L);
    }
}
