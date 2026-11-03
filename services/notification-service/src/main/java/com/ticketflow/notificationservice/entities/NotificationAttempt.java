package com.ticketflow.notificationservice.entities;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.time.Instant;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * Tentative d'envoi d'une notification (CdC §6.1 : reprises observables).
 *
 * Plusieurs tentatives par notification : reprise avec backoff exponentiel
 * (max-attempts configuré), chaque tentative est tracée ici pour audit.
 *
 * {@code @ManyToOne} est LAZY (NotificationRequest est chargé explicitement si besoin).
 */
@Entity
@Table(name = "notification_attempts",
        indexes = {
                @Index(name = "idx_notifatt_request", columnList = "notification_request_id"),
                @Index(name = "idx_notifatt_status", columnList = "status")
        })
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class NotificationAttempt {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(optional = false, fetch = FetchType.LAZY)
    @JoinColumn(name = "notification_request_id", nullable = false)
    private NotificationRequest request;

    @Column(name = "attempt_number", nullable = false)
    private int attemptNumber;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 15)
    private NotificationStatus status;

    /** Message d'erreur en cas d'échec (jamais de payload sensible). */
    @Column(name = "error_message", length = 1000)
    private String errorMessage;

    @Column(name = "attempted_at", nullable = false, updatable = false)
    private Instant attemptedAt;

    @Column(name = "duration_ms")
    private Long durationMs;

    public NotificationAttempt(NotificationRequest request, int attemptNumber,
                               NotificationStatus status, String errorMessage, Long durationMs) {
        this.request = request;
        this.attemptNumber = attemptNumber;
        this.status = status;
        this.errorMessage = errorMessage;
        this.durationMs = durationMs;
        this.attemptedAt = Instant.now();
    }
}
