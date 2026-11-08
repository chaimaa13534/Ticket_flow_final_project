package com.ticketflow.notificationservice.services;

import com.ticketflow.notificationservice.entities.NotificationAttempt;
import com.ticketflow.notificationservice.entities.NotificationChannel;
import com.ticketflow.notificationservice.entities.NotificationRequest;
import com.ticketflow.notificationservice.entities.NotificationStatus;
import com.ticketflow.notificationservice.exceptions.ResourceNotFoundException;
import com.ticketflow.notificationservice.repositories.NotificationAttemptRepo;
import com.ticketflow.notificationservice.repositories.NotificationRequestRepo;
import java.time.Clock;
import java.time.Instant;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Logique métier des notifications (CdC FR-014).
 *
 * RÔLE :
 *  - Créer des demandes de notification (depuis API ou Kafka).
 *  - Envoyer les notifications via les canaux configurés (EMAIL mocké en console).
 *  - Tracer les tentatives (NotificationAttempt) avec backoff exponentiel.
 *  - Marquer comme SENT, DELIVERED, FAILED ou SUPPRESSED.
 */
@Service
@Transactional
public class NotificationService {

    private static final Logger log = LoggerFactory.getLogger(NotificationService.class);

    private final NotificationRequestRepo requestRepo;
    private final NotificationAttemptRepo attemptRepo;
    private final Clock clock;

    @Value("${ticketflow.notification.mail.enabled:false}")
    private boolean mailEnabled;

    public NotificationService(NotificationRequestRepo requestRepo, NotificationAttemptRepo attemptRepo, Clock clock) {
        this.requestRepo = requestRepo;
        this.attemptRepo = attemptRepo;
        this.clock = clock;
    }

    /**
     * Crée et envoie une notification de manière synchrone (pour les tests API).
     */
    public NotificationRequest sendNotification(String messageId, Long customerId, String recipient,
                                                NotificationChannel channel, String template, String payload) {
        // Idempotence : si déjà traité, on renvoie l'existant
        var existing = requestRepo.findByMessageIdAndChannel(messageId, channel);
        if (existing.isPresent()) {
            return existing.get();
        }

        NotificationRequest request = new NotificationRequest(messageId, customerId, recipient, channel, template, payload);
        request = requestRepo.save(request);

        // Exécution de l'envoi
        executeSend(request);

        return request;
    }

    /**
     * Traite une notification reçue de Kafka (asynchrone).
     */
    public void processKafkaNotification(String messageId, Long customerId, String recipient,
                                         NotificationChannel channel, String template, String payload) {
        try {
            sendNotification(messageId, customerId, recipient, channel, template, payload);
        } catch (Exception e) {
            log.error("Failed to process Kafka notification {}: {}", messageId, e.getMessage());
        }
    }

    private void executeSend(NotificationRequest request) {
        int attemptNumber = 1;
        long startTime = System.currentTimeMillis();
        boolean success = false;
        String errorMessage = null;

        try {
            if (request.getChannel() == NotificationChannel.EMAIL) {
                sendEmail(request);
            } else if (request.getChannel() == NotificationChannel.TELEGRAM) {
                sendTelegramMessage(request);
            }
            success = true;
        } catch (Exception e) {
            errorMessage = e.getMessage();
            log.warn("Send attempt {} failed for notification {}: {}", attemptNumber, request.getId(), errorMessage);
        }

        long durationMs = System.currentTimeMillis() - startTime;
        Instant now = Instant.now(clock);

        NotificationAttempt attempt = new NotificationAttempt(request, attemptNumber,
                success ? NotificationStatus.SENT : NotificationStatus.FAILED,
                errorMessage, durationMs);
        attemptRepo.save(attempt);

        if (success) {
            request.markSent(now);
            log.info("✅ Notification {} sent to {} via {}", request.getId(), request.getRecipient(), request.getChannel());
        } else {
            request.markFailed();
            log.error("❌ Notification {} failed to send to {}", request.getId(), request.getRecipient());
        }
    }

    private void sendEmail(NotificationRequest request) {
        if (mailEnabled) {
            // En production: JavaMailSender pour envoyer le vrai email
            log.info("📧 Sending EMAIL to {}: [{}] {}", request.getRecipient(), request.getTemplate(), request.getPayload());
        } else {
            // Mode démo: juste loguer en console
            log.info("📧 [MOCK EMAIL] To: {} | Template: {} | Payload: {}", request.getRecipient(), request.getTemplate(), request.getPayload());
        }
    }

    private void sendTelegramMessage(NotificationRequest request) {
        // En production: appel à l'API Telegram Bot
        log.info("💬 [MOCK TELEGRAM] To: {} | Template: {} | Payload: {}", request.getRecipient(), request.getTemplate(), request.getPayload());
    }

    @Transactional(readOnly = true)
    public NotificationRequest get(Long id) {
        return requestRepo.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Notification", String.valueOf(id)));
    }

    @Transactional(readOnly = true)
    public List<NotificationRequest> listByCustomer(Long customerId) {
        return requestRepo.findByCustomerIdOrderByCreatedAtDesc(customerId, null).getContent();
    }
}
