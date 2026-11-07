package com.ticketflow.ticketservice.dto;

import com.ticketflow.ticketservice.entities.Ticket;
import com.ticketflow.ticketservice.entities.TicketStatus;
import java.time.Instant;

public record TicketResponse(
        Long id,
        String code,
        Long bookingId,
        Long ticketTierId,
        Long customerId,
        Long eventId,
        String eventTitle,
        Instant eventStartsAt,
        String tierName,
        TicketStatus status,
        Instant issuedAt,
        Instant usedAt
) {
    public static TicketResponse from(Ticket t) {
        return new TicketResponse(
                t.getId(),
                t.getCode(),
                t.getBookingId(),
                t.getTicketTierId(),
                t.getCustomerId(),
                t.getEventId(),
                t.getEventTitle(),
                t.getEventStartsAt(),
                t.getTierName(),
                t.getStatus(),
                t.getIssuedAt(),
                t.getUsedAt()
        );
    }
}
