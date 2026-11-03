package com.ticketflow.aiassistantservice.entities;

import com.ticketflow.aiassistantservice.enums.TelegramLinkStatus;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;

/**
 * Liaison Telegram <-> Customer dans le contexte de l'assistant IA.
 *
 * Différence avec TelegramLink du customer-service :
 *  - {@code customer-service}.TelegramLink : initiée depuis la session TicketFlow
 *    du client (code unique, vérification depuis Telegram).
 *  - {@code ai-assistant-service}.CustomerLink : vue locale utilisée par l'agent
 *    pour savoir si un utilisateur Telegram est lié à un compte métier.
 *
 * Les deux tables ne sont PAS synchronisées en base (pas de jointure cross-bases) :
 * la création d'une liaison côté customer-service publie un événement Kafka consommé
 * par ai-assistant-service qui crée/maj sa propre ligne ici.
 *
 * RÉ-LIAISON : seule une liaison VERIFIED est unique par customer_id et par telegram_user_id.
 * Après REVOKED, une nouvelle liaison peut être créée — imposé par index unique partiel côté BDD.
 *
 * CdC FR-017 : Telegram seul ne prouve jamais l'identité métier.
 */
@Entity
@Table(name = "customer_links")
public class CustomerLink {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "customer_id", nullable = false)
    private Long customerId;

    @Column(name = "telegram_user_id", nullable = false, length = 64)
    private String telegramUserId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private TelegramLinkStatus status;

    @Column(name = "linked_at", nullable = false, updatable = false)
    private Instant linkedAt;

    @Column(name = "revoked_at")
    private Instant revokedAt;

    protected CustomerLink() {
    }

    public CustomerLink(Long customerId, String telegramUserId) {
        this.customerId = customerId;
        this.telegramUserId = telegramUserId;
        this.status = TelegramLinkStatus.VERIFIED;
        this.linkedAt = Instant.now();
    }

    public void revoke() {
        this.status = TelegramLinkStatus.REVOKED;
        this.revokedAt = Instant.now();
    }

    // Getters
    public Long getId() { return id; }
    public Long getCustomerId() { return customerId; }
    public String getTelegramUserId() { return telegramUserId; }
    public TelegramLinkStatus getStatus() { return status; }
    public Instant getLinkedAt() { return linkedAt; }
    public Instant getRevokedAt() { return revokedAt; }
}