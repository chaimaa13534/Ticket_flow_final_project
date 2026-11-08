package com.ticketflow.notificationservice.dto;

import com.ticketflow.notificationservice.entities.NotificationChannel;
import com.ticketflow.notificationservice.entities.NotificationRequest;
import com.ticketflow.notificationservice.entities.NotificationStatus;
import java.time.Instant;

public record NotificationResponse(
        Long id,
        String messageId,
        Long customerId,
        String recipient,
        NotificationChannel channel,
        String template,
        NotificationStatus status,
        Instant createdAt,
        Instant sentAt
) {
    public static NotificationResponse from(NotificationRequest n) {
        return new NotificationResponse(
                n.getId(),
                n.getMessageId(),
                n.getCustomerId(),
                n.getRecipient(),
                n.getChannel(),
                n.getTemplate(),
                n.getStatus(),
                n.getCreatedAt(),
                n.getSentAt()
        );
    }
}
