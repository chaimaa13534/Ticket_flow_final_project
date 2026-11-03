package com.ticketflow.bookingservice.entities;

import com.ticketflow.bookingservice.exceptions.BookingRuleException;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import jakarta.persistence.Version;
import java.math.BigDecimal;
import java.time.Instant;

/**
 * Réservation (CdC §7.1 : Booking, FR-007 à FR-010).
 *
 * RÔLE :
 *  - Stocke l'état d'une demande de réservation (PENDING -> CONFIRMED -> ...).
 *  - Référence l'événement et le client (sans FK cross-bases — CdC §7.1).
 *  - {@code bookingReference} est l'identifiant stable exposé au client (ne pas exposer le PK).
 *  - La table dédiée {@code idempotency_keys} rend la création idempotente (FR-008 : pas de double réservation).
 *  - {@code expiresAt} est le TTL de la réservation PENDING (FR-008).
 *
 * Le prix total est un SNAPSHOT figé au moment de la réservation : il ne change pas si
 * l'événement est modifié par la suite (cohérence historique).
 *
 * {@code @Version} active l'optimistic locking : deux callbacks de paiement simultanés
 * ne peuvent pas confirmer deux fois la même réservation (CdC FR-010 : "La confirmation
 * survient une seule fois").
 */
@Entity
@Table(name = "bookings",
        indexes = {
                @Index(name = "uk_bookings_reference", columnList = "booking_reference", unique = true),
                @Index(name = "idx_bookings_customer", columnList = "customer_id"),
                @Index(name = "idx_bookings_event", columnList = "event_id"),
                @Index(name = "idx_bookings_status", columnList = "status"),
                @Index(name = "idx_bookings_expires_at", columnList = "expires_at")
        })
public class Booking {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** Référence stable exposée au client (ex : "BK-2026-000123"). */
    @Column(name = "booking_reference", nullable = false, unique = true, length = 30)
    private String bookingReference;

    /** Identifiant de l'événement côté event-service (pas de FK cross-bases). */
    @Column(name = "event_id", nullable = false)
    private Long eventId;

    /** Snapshot du titre de l'événement au moment de la réservation. */
    @Column(name = "event_title", nullable = false, length = 150)
    private String eventTitle;

    /** Identifiant du client côté customer-service (pas de FK cross-bases). */
    @Column(name = "customer_id", nullable = false)
    private Long customerId;

    // NOTE : idempotency_key est stockée dans la table dédiée idempotency_keys (source de vérité).
    // Pas de duplication ici (CdC §7.1).

    @Column(nullable = false, precision = 12, scale = 2)
    private BigDecimal totalAmount;

    @Column(nullable = false, length = 3)
    private String currency = "MAD";

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private BookingStatus status = BookingStatus.PENDING;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    /** TTL d'une réservation PENDING. Si null et statut PENDING, jamais expirée (test only). */
    @Column(name = "expires_at")
    private Instant expiresAt;

    @Column(name = "confirmed_at")
    private Instant confirmedAt;

    @Column(name = "cancelled_at")
    private Instant cancelledAt;

    @Column(name = "expired_at")
    private Instant expiredAt;

    /**
     * Optimistic locking : protège la transition PENDING -> CONFIRMED contre
     * deux callbacks de paiement simultanés (CdC FR-010).
     */
    @Version
    @Column(name = "version", nullable = false)
    private Long version = 0L;

    protected Booking() {
    }

    public Booking(String bookingReference, Long eventId, String eventTitle, Long customerId,
                   BigDecimal totalAmount, Instant createdAt, Instant expiresAt) {
        this.bookingReference = bookingReference;
        this.eventId = eventId;
        this.eventTitle = eventTitle;
        this.customerId = customerId;
        this.totalAmount = totalAmount;
        this.createdAt = createdAt;
        this.expiresAt = expiresAt;
    }

    /** PENDING -> CONFIRMED. Unique, après paiement admissible (CdC FR-010). */
    public void confirm(Instant when) {
        if (status != BookingStatus.PENDING) {
            throw new BookingRuleException(
                    "Only a PENDING booking can be confirmed, but it is " + status);
        }
        this.status = BookingStatus.CONFIRMED;
        this.confirmedAt = when;
    }

    /** PENDING or CONFIRMED -> CANCELLED (CdC FR-012). */
    public void cancel(Instant when) {
        if (status == BookingStatus.CANCELLED) {
            throw new BookingRuleException("The booking is already cancelled");
        }
        if (status == BookingStatus.EXPIRED) {
            throw new BookingRuleException("An expired booking cannot be cancelled");
        }
        if (status == BookingStatus.REFUNDED) {
            throw new BookingRuleException("A refunded booking cannot be cancelled");
        }
        this.status = BookingStatus.CANCELLED;
        this.cancelledAt = when;
    }

    /** PENDING -> EXPIRED (TTL écoulé sans paiement, FR-008). */
    public void expire(Instant when) {
        if (status != BookingStatus.PENDING) {
            throw new BookingRuleException(
                    "Only a PENDING booking can be expired, but it is " + status);
        }
        this.status = BookingStatus.EXPIRED;
        this.expiredAt = when;
    }

    /** CONFIRMED -> REFUNDED (CdC FR-012 : annulation post-confirmation). */
    public void refund(Instant when) {
        if (status != BookingStatus.CONFIRMED && status != BookingStatus.CANCELLED) {
            throw new BookingRuleException(
                    "Only a CONFIRMED or CANCELLED booking can be refunded, but it is " + status);
        }
        this.status = BookingStatus.REFUNDED;
        this.cancelledAt = this.cancelledAt != null ? this.cancelledAt : when;
    }


    public Long getId() { return id; }
    public String getBookingReference() { return bookingReference; }
    public Long getEventId() { return eventId; }
    public String getEventTitle() { return eventTitle; }
    public Long getCustomerId() { return customerId; }
    public BigDecimal getTotalAmount() { return totalAmount; }
    public String getCurrency() { return currency; }
    public BookingStatus getStatus() { return status; }
    public Instant getCreatedAt() { return createdAt; }
    public Instant getExpiresAt() { return expiresAt; }
    public Instant getConfirmedAt() { return confirmedAt; }
    public Instant getCancelledAt() { return cancelledAt; }
    public Instant getExpiredAt() { return expiredAt; }
    public Long getVersion() { return version; }
}
