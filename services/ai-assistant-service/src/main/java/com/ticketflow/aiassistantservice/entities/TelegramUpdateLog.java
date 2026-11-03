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

/**
 * Log de déduplication des updates Telegram (CdC FR-019).
 *
 * Telegram renvoie parfois le même update_id plusieurs fois (retries réseau).
 * Cette table garantit qu'un update_id n'est traité qu'une seule fois :
 *  - Premier appel : INSERT réussit, statut = RECEIVED puis PROCESSED.
 *  - Appel suivant : INSERT échoue (contrainte unique), on renvoie DUPLICATE.
 */
@Entity
@Table(name = "telegram_update_logs",
        indexes = {
                @Index(name = "uk_tgupd_update_id", columnList = "update_id", unique = true),
                @Index(name = "idx_tgupd_telegram_user", columnList = "telegram_user_id"),
                @Index(name = "idx_tgupd_received_at", columnList = "received_at")
        })
public class TelegramUpdateLog {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** Identifiant unique Telegram — garantie de déduplication. */
    @Column(name = "update_id", nullable = false, unique = true)
    private Long updateId;

    @Column(name = "telegram_user_id", length = 64)
    private String telegramUserId;

    @Column(name = "received_at", nullable = false, updatable = false)
    private Instant receivedAt;

    @Column(name = "processed_at")
    private Instant processedAt;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 15)
    private TelegramUpdateStatus status = TelegramUpdateStatus.RECEIVED;

    /** Taille du payload (anti-abus : on rejette les messages trop longs). */
    @Column(name = "payload_size_bytes")
    private Integer payloadSizeBytes;

    protected TelegramUpdateLog() {
    }

    public TelegramUpdateLog(Long updateId, String telegramUserId, Integer payloadSizeBytes) {
        this.updateId = updateId;
        this.telegramUserId = telegramUserId;
        this.payloadSizeBytes = payloadSizeBytes;
        this.receivedAt = Instant.now();
    }

    public void markProcessed(Instant when) {
        this.status = TelegramUpdateStatus.PROCESSED;
        this.processedAt = when;
    }

    public void markDuplicate() {
        this.status = TelegramUpdateStatus.DUPLICATE;
    }

    public void markUnsupported() {
        this.status = TelegramUpdateStatus.UNSUPPORTED;
        this.processedAt = Instant.now();
    }

    // Getters
    public Long getId() { return id; }
    public Long getUpdateId() { return updateId; }
    public String getTelegramUserId() { return telegramUserId; }
    public Instant getReceivedAt() { return receivedAt; }
    public Instant getProcessedAt() { return processedAt; }
    public TelegramUpdateStatus getStatus() { return status; }
    public Integer getPayloadSizeBytes() { return payloadSizeBytes; }
}