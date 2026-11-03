package com.ticketflow.notificationservice.repositories;

import com.ticketflow.notificationservice.entities.NotificationRequest;
import com.ticketflow.notificationservice.entities.NotificationStatus;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface NotificationRequestRepo extends JpaRepository<NotificationRequest, Long> {

    /**
     * Idempotence par (messageId, channel) : un même événement Kafka ne crée qu'une seule
     * notification PAR canal — permet le multicanal (email + Telegram pour le même messageId).
     */
    Optional<NotificationRequest> findByMessageIdAndChannel(String messageId,
                                                            com.ticketflow.notificationservice.entities.NotificationChannel channel);

    Page<NotificationRequest> findByCustomerIdOrderByCreatedAtDesc(Long customerId, Pageable pageable);

    /** Notifications PENDING prêtes à être envoyées (scheduled_at <= now). */
    List<NotificationRequest> findByStatusAndScheduledAtBeforeOrStatusAndScheduledAtIsNull(
            NotificationStatus scheduledStatus, Instant when,
            NotificationStatus pendingStatus);

    List<NotificationRequest> findByStatus(NotificationStatus status);
}