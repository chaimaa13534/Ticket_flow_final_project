package com.ticketflow.notificationservice.controllers;

import com.ticketflow.notificationservice.dto.NotificationResponse;
import com.ticketflow.notificationservice.entities.NotificationChannel;
import com.ticketflow.notificationservice.entities.NotificationRequest;
import com.ticketflow.notificationservice.services.NotificationService;
import java.util.List;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * API REST Notification — préfixe /api/v1 (CdC §7.2).
 *
 * Endpoints :
 *  - POST /api/v1/notifications/send : envoyer une notification (test manuel)
 *  - GET /api/v1/notifications/{id} : consulter
 *  - GET /api/v1/notifications/by-customer/{customerId} : lister par client
 */
@RestController
@RequestMapping("/api/v1/notifications")
public class NotificationController {

    private final NotificationService service;

    public NotificationController(NotificationService service) {
        this.service = service;
    }

    @PostMapping("/send")
    public ResponseEntity<NotificationResponse> send(@RequestBody SendNotificationRequest req) {
        NotificationRequest notification = service.sendNotification(
                req.messageId(),
                req.customerId(),
                req.recipient(),
                req.channel(),
                req.template(),
                req.payload()
        );
        return ResponseEntity.ok(NotificationResponse.from(notification));
    }

    @GetMapping("/{id}")
    public NotificationResponse get(@PathVariable Long id) {
        return NotificationResponse.from(service.get(id));
    }

    @GetMapping("/by-customer/{customerId}")
    public List<NotificationResponse> listByCustomer(@PathVariable Long customerId) {
        return service.listByCustomer(customerId).stream()
                .map(NotificationResponse::from)
                .toList();
    }

    // DTO interne pour la requête d'envoi
    public record SendNotificationRequest(
            String messageId,
            Long customerId,
            String recipient,
            NotificationChannel channel,
            String template,
            String payload
    ) {}
}
