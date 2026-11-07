package com.ticketflow.bookingservice.dto;

import com.ticketflow.bookingservice.entities.Booking;
import com.ticketflow.bookingservice.entities.BookingLine;
import com.ticketflow.bookingservice.entities.BookingStatus;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

public record BookingResponse(
        Long id,
        String bookingReference,
        Long eventId,
        String eventTitle,
        Long customerId,
        BigDecimal totalAmount,
        String currency,
        BookingStatus status,
        Instant createdAt,
        Instant expiresAt,
        Instant confirmedAt,
        List<LineResponse> lines
) {
    public static BookingResponse from(Booking b, List<BookingLine> lines) {
        return new BookingResponse(
                b.getId(),
                b.getBookingReference(),
                b.getEventId(),
                b.getEventTitle(),
                b.getCustomerId(),
                b.getTotalAmount(),
                b.getCurrency(),
                b.getStatus(),
                b.getCreatedAt(),
                b.getExpiresAt(),
                b.getConfirmedAt(),
                lines.stream().map(LineResponse::from).toList()
        );
    }

    public record LineResponse(
            Long id,
            Long ticketTierId,
            String tierName,
            BigDecimal unitPrice,
            int quantity,
            BigDecimal lineTotal
    ) {
        public static LineResponse from(BookingLine l) {
            return new LineResponse(l.getId(), l.getTicketTierId(), l.getTierName(),
                    l.getUnitPrice(), l.getQuantity(), l.getLineTotal());
        }
    }
}
