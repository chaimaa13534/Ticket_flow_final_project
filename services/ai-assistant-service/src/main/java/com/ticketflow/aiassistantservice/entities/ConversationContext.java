package com.ticketflow.aiassistantservice.entities;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.UUID;

/**
 * Contexte conversationnel à durée limitée (CdC FR-024).
 *
 * IDENTIFIANT OPAQUE : {@code conversationToken} est un UUID v4 exposé à l'agent et au
 * client. La PK numérique {@code id} n'est JAMAIS partagée hors du service. Cela évite
 * qu'un acteur externe puisse énumérer ou deviner des conversations.
 *
 * Le contexte n'est PAS le contenu brut des messages : c'est un identifiant opaque
 * (uniquement l'historique synthétisé) avec TTL, pour ne pas persister
 * indéfiniment les conversations des utilisateurs (CdC §6.1).
 *
 * Réinitialisable par l'utilisateur à tout moment (FR-024).
 *
 * AUDIT NON CASCADE : {@code tool_audit_logs} référence cette entité via
 * {@code conversation_token} (pas de FK cascade) pour que les audits survivent à la purge
 * des contextes expirés (CdC FR-020).
 */
@Entity
@Table(name = "conversation_contexts",
        indexes = {
                @Index(name = "uk_ctx_telegram_user", columnList = "telegram_user_id", unique = true),
                @Index(name = "idx_ctx_expires_at", columnList = "expires_at")
        })
public class ConversationContext {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** Token opaque UUID v4 — exposé à l'agent et au client. Jamais la PK numérique. */
    @Column(name = "conversation_token", nullable = false, unique = true, length = 36)
    private String conversationToken;

    @Column(name = "telegram_user_id", nullable = false, unique = true, length = 64)
    private String telegramUserId;

    /** Référence au CustomerLink si l'utilisateur a lié son compte (sinon null). */
    @Column(name = "customer_link_id")
    private Long customerLinkId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private ConversationState state = ConversationState.INIT;

    /** Historique synthétisé (JSON) — limité en taille, jamais le contenu brut. */
    @Column(name = "history", length = 8000)
    private String history;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    @Column(name = "expires_at", nullable = false)
    private Instant expiresAt;

    protected ConversationContext() {
    }

    public ConversationContext(String telegramUserId, Instant expiresAt) {
        this.conversationToken = UUID.randomUUID().toString();
        this.telegramUserId = telegramUserId;
        this.expiresAt = expiresAt;
        Instant now = Instant.now();
        this.createdAt = now;
        this.updatedAt = now;
    }

    public void setState(ConversationState state) {
        this.state = state;
        this.updatedAt = Instant.now();
    }

    public void setHistory(String history) {
        this.history = history;
        this.updatedAt = Instant.now();
    }

    public void attachCustomerLink(Long customerLinkId) {
        this.customerLinkId = customerLinkId;
        this.updatedAt = Instant.now();
    }

    /** Réinitialisation demandée par l'utilisateur (FR-024) — génère un nouveau token. */
    public void reset(Instant newExpiresAt) {
        this.conversationToken = UUID.randomUUID().toString();
        this.state = ConversationState.INIT;
        this.history = null;
        this.customerLinkId = null;
        this.expiresAt = newExpiresAt;
        this.updatedAt = Instant.now();
    }

    public boolean isExpired() {
        return Instant.now().isAfter(expiresAt);
    }

    // Getters
    public Long getId() { return id; }
    public String getConversationToken() { return conversationToken; }
    public String getTelegramUserId() { return telegramUserId; }
    public Long getCustomerLinkId() { return customerLinkId; }
    public ConversationState getState() { return state; }
    public String getHistory() { return history; }
    public Instant getCreatedAt() { return createdAt; }
    public Instant getUpdatedAt() { return updatedAt; }
    public Instant getExpiresAt() { return expiresAt; }
}
