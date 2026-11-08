package com.ticketflow.aiassistantservice.services;

import com.ticketflow.aiassistantservice.clients.McpClient;
import com.ticketflow.aiassistantservice.clients.McpClient.EventView;
import com.ticketflow.aiassistantservice.entities.ConversationContext;
import com.ticketflow.aiassistantservice.entities.TelegramUpdateLog;
import com.ticketflow.aiassistantservice.repositories.TelegramUpdateLogRepo;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;


/**
 * Orchestrateur de l'assistant IA (CdC §6.1, FR-015 à FR-019).
 *
 * Mode MOCK (sans clé API LLM) :
 *  - Si l'utilisateur tape "concert", "événement", "recherche" → appelle search_public_events
 *  - Si l'utilisateur tape un numéro → appelle get_public_event
 *  - Si l'utilisateur tape "reset" → réinitialise le contexte
 *  - Sinon → réponse par défaut
 */
@Service
@Transactional
public class AiAssistantService {

    private static final Logger log = LoggerFactory.getLogger(AiAssistantService.class);

    private final McpClient mcpClient;
    private final ConversationService conversationService;
    private final TelegramUpdateLogRepo updateLogRepo;

    public AiAssistantService(McpClient mcpClient, ConversationService conversationService,
                              TelegramUpdateLogRepo updateLogRepo) {
        this.mcpClient = mcpClient;
        this.conversationService = conversationService;
        this.updateLogRepo = updateLogRepo;
    }

    /**
     * Traite un message reçu de Telegram et génère une réponse.
     */
    public String processMessage(Long updateId, String telegramUserId, String text) {
        log.info("Processing message from Telegram user {}: {}", telegramUserId, text);

        // 1. Déduplication (FR-019)
        if (updateLogRepo.existsByUpdateId(updateId)) {
            log.info("Duplicate update {}, ignoring", updateId);
            return null; // Ignorer les doublons
        }

        // Logge l'update
        TelegramUpdateLog updateLog = new TelegramUpdateLog(updateId, telegramUserId, text.length());
        updateLogRepo.save(updateLog);

        // 2. Gère le contexte conversationnel
        ConversationContext ctx = conversationService.getOrCreateContext(telegramUserId);

        // 3. Analyse l'intention (Mock LLM)
        String response = analyzeIntentAndRespond(text, ctx);

        // 4. Met à jour l'historique
        conversationService.updateHistory(ctx, text, response);

        // 5. Marque l'update comme traité
        updateLog.markProcessed(java.time.Instant.now());

        return response;
    }

    /**
     * Analyse l'intention du message et génère une réponse (Mock LLM).
     */
    private String analyzeIntentAndRespond(String text, ConversationContext ctx) {
        String lowerText = text.toLowerCase().trim();

        // Commande : reset
        if (lowerText.equals("reset") || lowerText.equals("/reset")) {
            conversationService.resetContext(ctx.getTelegramUserId());
            return "✅ Votre contexte conversationnel a été réinitialisé. Posez-moi une nouvelle question !";
        }

        // Commande : aide
        if (lowerText.equals("/help") || lowerText.equals("aide")) {
            return """
                    🤖 Assistant TicketFlow
                    
                    Je peux vous aider avec :
                    • "concert" ou "événement" - Rechercher des événements
                    • Un numéro (ex: "1") - Voir les détails d'un événement
                    • "reset" - Réinitialiser notre conversation
                    
                    Que voulez-vous savoir ?""";
        }

        // Si l'utilisateur tape un numéro → get_public_event
        try {
            Long eventId = Long.parseLong(lowerText);
            return getEventDetails(eventId);
        } catch (NumberFormatException ignored) {
            // Ce n'est pas un numéro, on continue
        }

        // Si le texte contient des mots-clés de recherche
        if (lowerText.contains("concert") || lowerText.contains("événement")
                || lowerText.contains("event") || lowerText.contains("recherche")
                || lowerText.contains("trouve") || lowerText.contains("voir")) {
            return searchEvents();
        }

        // Réponse par défaut
        return """
                🤖 Je n'ai pas bien compris votre demande.
                
                Essayez de taper :
                • "concert" pour voir les événements disponibles
                • Un numéro pour voir les détails d'un événement
                • "/help" pour voir l'aide
                """;
    }

    /**
     * Outil : search_public_events (FR-015).
     */
    private String searchEvents() {
        try {
            List<EventView> events = mcpClient.searchPublicEvents(0, 5);
            if (events == null || events.isEmpty()) {
                return "Aucun événement public trouvé pour le moment. Revenez plus tard !";
            }

            StringBuilder sb = new StringBuilder("🎭 Événements disponibles :\n\n");
            for (EventView e : events) {
                sb.append(String.format("• [%d] %s\n  📍 %s, %s\n  📅 %s\n  💰 %s MAD\n\n",
                        e.id(),
                        e.title(),
                        e.venueName() != null ? e.venueName() : "Lieu non spécifié",
                        e.venueCity() != null ? e.venueCity() : "",
                        e.startsAt() != null ? e.startsAt().toString().substring(0, 10) : "Date non définie",
                        e.basePrice() != null ? e.basePrice().toString() : "Prix non défini"
                ));
            }
            sb.append("\nTapez un numéro pour voir les détails d'un événement.");
            return sb.toString();
        } catch (Exception e) {
            log.error("Failed to search events: {}", e.getMessage());
            return "⚠️ Impossible de récupérer les événements pour le moment. Réessayez plus tard.";
        }
    }

    /**
     * Outil : get_public_event (FR-015).
     */
    private String getEventDetails(Long eventId) {
        try {
            EventView event = mcpClient.getPublicEvent(eventId);
            StringBuilder sb = new StringBuilder("🎭 Détails de l'événement :\n\n");
            sb.append(String.format("Titre : %s\n", event.title()));
            if (event.description() != null) {
                sb.append(String.format("Description : %s\n", event.description()));
            }
            if (event.venueName() != null) {
                sb.append(String.format("Lieu : %s, %s\n", event.venueName(),
                        event.venueCity() != null ? event.venueCity() : ""));
            }
            if (event.startsAt() != null) {
                sb.append(String.format("Date : %s\n", event.startsAt().toString()));
            }
            if (event.basePrice() != null) {
                sb.append(String.format("Prix : %s MAD\n", event.basePrice()));
            }
            sb.append("\nPour réserver, rendez-vous sur TicketFlow ! 🎫");
            return sb.toString();
        } catch (Exception e) {
            log.error("Failed to get event {}: {}", eventId, e.getMessage());
            return "⚠️ Événement introuvable ou service indisponible. Vérifiez le numéro et réessayez.";
        }
    }
}
