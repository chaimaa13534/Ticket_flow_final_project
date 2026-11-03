package com.ticketflow.notificationservice.entities;

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
 * Demande de notification (CdC §7.1, FR-014).
 *
 * Créée à partir d'un événement Kafka (BookingConfirmed, PaymentFailed, etc.) ou par
 * appel direct d'un service métier. {@code messageId} est l'identifiant Kafka du message
 * source : il rend le consommateur idempotent (CdC §6.1 : "consommateurs idempotents").
 *
 * MULTICANAL (CdC FR-014) : un MÊME événement Kafka peut déclencher plusieurs notifications
 * sur des canaux différents (email + Telegram). L'unicité est donc (messageId, channel),
 * pas seulement messageId.
 */
@Entity
@Table(name = "notification_requests",
        indexes = {
                @Index(name = "uk_notif_msg_channel", columnList = "message_id, channel", unique = true),
                @Index(name = "idx_notif_customer", columnList = "customer_id"),
                @Index(name = "idx_notif_status", columnList = "status"),
                @Index(name = "idx_notif_channel", columnList = "channel"),
                @Index(name = "idx_notif_scheduled_at", columnList = "scheduled_at")
        })
public class NotificationRequest {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** Identifiant unique du message Kafka source — clé d'idempotence combinée au canal. */
    @Column(name = "message_id", nullable = false, length = 100)
    private String messageId;

    @Column(name = "customer_id")
    private Long customerId;

    @Column(nullable = false, length = 254)
    private String recipient;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 15)
    private NotificationChannel channel;

    /** Template de contenu (ex : "booking.confirmed", "payment.failed"). */
    @Column(nullable = false, length = 80)
    private String template;

    /** Payload JSON contenant les variables du template. */
    @Column(nullable = false, length = 4000)
    private String payload;

    /** Locale du destinataire (pour la traduction). */
    @Column(length = 10)
    private String locale = "fr";

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 15)
    private NotificationStatus status = NotificationStatus.PENDING;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "scheduled_at")
    private Instant scheduledAt;

    @Column(name = "sent_at")
    private Instant sentAt;

    @Column(name = "delivered_at")
    private Instant deliveredAt;

    protected NotificationRequest() {
    }

    public NotificationRequest(String messageId, Long customerId, String recipient,
                               NotificationChannel channel, String template, String payload) {
        this.messageId = messageId;
        this.customerId = customerId;
        this.recipient = recipient;
        this.channel = channel;
        this.template = template;
        this.payload = payload;
        this.createdAt = Instant.now();
    }

    public void markSent(Instant when) {
        this.status = NotificationStatus.SENT;
        this.sentAt = when;
    }

    public void markDelivered(Instant when) {
        this.status = NotificationStatus.DELIVERED;
        this.deliveredAt = when;
    }

    public void markFailed() {
        this.status = NotificationStatus.FAILED;
    }

    public void suppress() {
        this.status = NotificationStatus.SUPPRESSED;
    }

    // Getters
    public Long getId() { return id; }
    public String getMessageId() { return messageId; }
    public Long getCustomerId() { return customerId; }
    public String getRecipient() { return recipient; }
    public NotificationChannel getChannel() { return channel; }
    public String getTemplate() { return template; }
    public String getPayload() { return payload; }
    public String getLocale() { return locale; }
    public void setLocale(String locale) { this.locale = locale; }
    public NotificationStatus getStatus() { return status; }
    public Instant getCreatedAt() { return createdAt; }
    public Instant getScheduledAt() { return scheduledAt; }
    public void scheduleAt(Instant when) { this.scheduledAt = when; }
    public Instant getSentAt() { return sentAt; }
    public Instant getDeliveredAt() { return deliveredAt; }
}