package com.ticketflow.paymentservice.dto;

import com.ticketflow.paymentservice.entities.PaymentAttempt;
import com.ticketflow.paymentservice.entities.PaymentMode;
import com.ticketflow.paymentservice.entities.PaymentStatus;
import java.math.BigDecimal;
import java.time.Instant;

public record PaymentResponse(
        Long id,
        Long bookingId,
        Long customerId,
        BigDecimal amount,
        PaymentStatus status,
        PaymentMode mode,
        Instant createdAt,
        Instant processedAt
) {
    public static PaymentResponse from(PaymentAttempt p) {
        return new PaymentResponse(
                p.getId(),
                p.getBookingId(),
                p.getCustomerId(),
                p.getAmount(),
                p.getStatus(),
                p.getMode(),
                p.getCreatedAt(),
                p.getProcessedAt()
        );
    }
}
