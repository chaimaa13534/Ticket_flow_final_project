package com.ticketflow.aiassistantservice.controllers;

import com.ticketflow.aiassistantservice.services.AiAssistantService;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Webhook Telegram (CdC FR-019).
 *
 * Reçoit les updates Telegram via POST /webhooks/telegram.
 * L'API Gateway route /webhooks/telegram/** vers ce controller.
 *
 * Format simplifié pour la démo (sans validation de signature Telegram).
 */
@RestController
@RequestMapping("/webhooks/telegram")
public class TelegramWebhookController {

    private static final Logger log = LoggerFactory.getLogger(TelegramWebhookController.class);

    private final AiAssistantService assistantService;

    public TelegramWebhookController(AiAssistantService assistantService) {
        this.assistantService = assistantService;
    }

    /**
     * Reçoit un update Telegram.
     *
     * Body attendu (format simplifié) :
     * {
     *   "updateId": 123456,
     *   "telegramUserId": "98765432",
     *   "text": "concert"
     * }
     */
    @PostMapping
    public ResponseEntity<Map<String, String>> receiveUpdate(@RequestBody Map<String, Object> body) {
        log.info("Received Telegram webhook: {}", body);

        try {
            Long updateId = ((Number) body.get("updateId")).longValue();
            String telegramUserId = String.valueOf(body.get("telegramUserId"));
            String text = String.valueOf(body.get("text"));

            String response = assistantService.processMessage(updateId, telegramUserId, text);

            if (response == null) {
                // Doublon → on répond quand même 200 OK
                return ResponseEntity.ok(Map.of("status", "DUPLICATE"));
            }

            return ResponseEntity.ok(Map.of(
                    "status", "OK",
                    "response", response
            ));
        } catch (Exception e) {
            log.error("Error processing Telegram update: {}", e.getMessage(), e);
            return ResponseEntity.ok(Map.of(
                    "status", "ERROR",
                    "response", "Une erreur est survenue. Tapez /help pour l'aide."
            ));
        }
    }
}
