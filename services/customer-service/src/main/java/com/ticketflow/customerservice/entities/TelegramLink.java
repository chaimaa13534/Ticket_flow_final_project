package com.ticketflow.customerservice.entities;

import com.ticketflow.customerservice.enums.TelegramLinkStatus;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.Version;
import java.time.Instant;

/**
 * Liaison entre un compte TicketFlow et un compte Telegram (CdC FR-017).
 *
 * RÈGLES D'OR (CdC §2.3 et §7.4) :
 *  - La liaison est initiée depuis une session TicketFlow authentifiée.
 *  - Le code de liaison ({@code linkCode}) est unique, à usage unique, durée courte.
 *  - Telegram seul ne prouve JAMAIS l'identité métier : tant que le code n'est pas validé,
 *    le statut reste PENDING et l'assistant ne peut pas accéder aux réservations du client.
 *
 * RÉ-LIAISON (FR-017) :
 *  Après REVOKED ou EXPIRED, l'utilisateur peut initier une NOUVELLE liaison.
 *  L'unicité est donc PARTIELLE : seule une liaison PENDING ou VERIFIED est unique par client.
 *  Voir la migration Flyway V001 pour l'index unique partiel PostgreSQL.
 *
 * @Version : protège contre la double validation concurrente d'un même code
 * (deux webhooks Telegram simultanés ne peuvent pas valider le même PENDING).
 */
@Entity
@Table(name = "telegram_links")
public class TelegramLink {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "customer_id", nullable = false)
    private Long customerId;

    /** Identifiant Telegram de l'utilisateur (chat_id distinct car un user peut changer de chat). */
    @Column(name = "telegram_user_id", length = 64)
    private String telegramUserId;

    @Column(name = "telegram_chat_id", length = 64)
    private String telegramChatId;

    /** Code unique à usage unique, généré côté TicketFlow. */
    @Column(name = "link_code", nullable = false, unique = true, length = 32)
    private String linkCode;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    private TelegramLinkStatus status = TelegramLinkStatus.PENDING;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "expires_at", nullable = false)
    private Instant expiresAt;

    @Column(name = "verified_at")
    private Instant verifiedAt;

    @Column(name = "revoked_at")
    private Instant revokedAt;

    /** Optimistic locking — protège la transition PENDING -> VERIFIED contre la concurrence. */
    @Version
    @Column(name = "version", nullable = false)
    private Long version = 0L;

    protected TelegramLink() {
    }

    public TelegramLink(Long customerId, String linkCode, Instant expiresAt) {
        this.customerId = customerId;
        this.linkCode = linkCode;
        this.expiresAt = expiresAt;
        this.createdAt = Instant.now();
    }

    /** Confirme la liaison depuis Telegram. */
    public void verify(String telegramUserId, String telegramChatId) {
        if (status != TelegramLinkStatus.PENDING) {
            throw new IllegalStateException("Only a PENDING link can be verified, but it is " + status);
        }
        if (Instant.now().isAfter(expiresAt)) {
            this.status = TelegramLinkStatus.EXPIRED;
            throw new IllegalStateException("Link code has expired");
        }
        this.telegramUserId = telegramUserId;
        this.telegramChatId = telegramChatId;
        this.status = TelegramLinkStatus.VERIFIED;
        this.verifiedAt = Instant.now();
    }

    public void revoke() {
        this.status = TelegramLinkStatus.REVOKED;
        this.revokedAt = Instant.now();
    }

    public void expireIfDue() {
        if (status == TelegramLinkStatus.PENDING && Instant.now().isAfter(expiresAt)) {
            this.status = TelegramLinkStatus.EXPIRED;
        }
    }

    // Getters
    public Long getId() { return id; }
    public Long getCustomerId() { return customerId; }
    public String getTelegramUserId() { return telegramUserId; }
    public String getTelegramChatId() { return telegramChatId; }
    public String getLinkCode() { return linkCode; }
    public TelegramLinkStatus getStatus() { return status; }
    public Instant getCreatedAt() { return createdAt; }
    public Instant getExpiresAt() { return expiresAt; }
    public Instant getVerifiedAt() { return verifiedAt; }
    public Instant getRevokedAt() { return revokedAt; }
    public Long getVersion() { return version; }
}

