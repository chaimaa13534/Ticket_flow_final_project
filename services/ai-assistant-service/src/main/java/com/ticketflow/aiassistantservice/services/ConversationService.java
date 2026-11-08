package com.ticketflow.aiassistantservice.services;

import com.ticketflow.aiassistantservice.entities.ConversationContext;
import com.ticketflow.aiassistantservice.entities.ConversationState;
import com.ticketflow.aiassistantservice.repositories.ConversationContextRepo;
import java.time.Clock;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Optional;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Gère le contexte conversationnel (CdC FR-024).
 *
 * - Crée ou récupère le contexte d'un utilisateur Telegram.
 * - Gère le TTL (expiration après 30 minutes d'inactivité).
 * - Permet la réinitialisation du contexte par l'utilisateur.
 */
@Service
@Transactional
public class ConversationService {

    private static final Logger log = LoggerFactory.getLogger(ConversationService.class);

    private final ConversationContextRepo repo;
    private final Clock clock;

    @Value("${ticketflow.assistant.context.ttl-minutes:30}")
    private int ttlMinutes;

    public ConversationService(ConversationContextRepo repo, Clock clock) {
        this.repo = repo;
        this.clock = clock;
    }

    /**
     * Récupère ou crée le contexte d'un utilisateur Telegram.
     */
    public ConversationContext getOrCreateContext(String telegramUserId) {
        Optional<ConversationContext> existing = repo.findByTelegramUserId(telegramUserId);

        if (existing.isPresent()) {
            ConversationContext ctx = existing.get();
            // Vérifie si le contexte a expiré
            if (ctx.isExpired()) {
                log.info("Context expired for user {}, resetting", telegramUserId);
                ctx.reset(Instant.now(clock).plus(ttlMinutes, ChronoUnit.MINUTES));
            }
            return ctx;
        }

        // Crée un nouveau contexte
        ConversationContext ctx = new ConversationContext(
                telegramUserId,
                Instant.now(clock).plus(ttlMinutes, ChronoUnit.MINUTES)
        );
        ctx.setState(ConversationState.AWAITING_QUERY);
        return repo.save(ctx);
    }

    /**
     * Met à jour l'historique de la conversation.
     */
    public void updateHistory(ConversationContext ctx, String userMessage, String botResponse) {
        String history = ctx.getHistory();
        if (history == null) history = "";
        // Garde seulement les 10 derniers échanges (limite de taille)
        String newEntry = "User: " + userMessage + "\nBot: " + botResponse + "\n---\n";
        history = history + newEntry;
        if (history.length() > 8000) {
            history = history.substring(history.length() - 8000);
        }
        ctx.setHistory(history);
        ctx.setState(ConversationState.COMPLETED);
    }

    /**
     * Réinitialise le contexte (FR-024).
     */
    public void resetContext(String telegramUserId) {
        repo.findByTelegramUserId(telegramUserId).ifPresent(ctx -> {
            ctx.reset(Instant.now(clock).plus(ttlMinutes, ChronoUnit.MINUTES));
        });
    }
}
