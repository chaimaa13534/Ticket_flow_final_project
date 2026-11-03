package com.ticketflow.ticketservice.entities;

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
import java.time.Instant;

/**
 * Billet unique émis après confirmation de réservation (CdC §7.1 et FR-011).
 *
 * RÈGLES :
 *  - {@code code} est NON prédictible (UUID + HMAC, généré côté service) — ne contient
 *    AUCUNE donnée personnelle (CdC §3.5).
 *  - {@code bookingId} référence le booking côté booking-service (pas de FK cross-bases).
 *  - {@code customerId} est l'identité issue du JWT (FR-013 : jamais les billets d'autrui).
 *  - {@code eventId} est un snapshot pour permettre la recherche par événement.
 *
 * Un billet par unité réservée : une réservation de 3 places génère 3 tickets.
 *
 * UNICITÉ (anti-rejeu Kafka) : un même événement Kafka rejoué ne doit pas générer deux fois
 * les mêmes billets. La contrainte UNIQUE (booking_id, ticket_tier_id) imposée côté BDD
 * garantit qu'un palier donné d'une réservation ne peut avoir qu'un seul billet.
 * (Dans le cas où une réservation a plusieurs unités du même palier, l'application
 * génère un billet par unité avec un numéro de séquence ajouté à la clé d'unicité —
 * implémenté à l'étape 3.)
 *
 * {@code @Version} protège les transitions ISSUED -> USED contre la concurrence.
 */
@Entity
@Table(name = "tickets",
        indexes = {
                @Index(name = "uk_tickets_code", columnList = "code", unique = true),
                @Index(name = "uk_tickets_booking_tier", columnList = "booking_id, ticket_tier_id", unique = true),
                @Index(name = "idx_tickets_customer", columnList = "customer_id"),
                @Index(name = "idx_tickets_event", columnList = "event_id"),
                @Index(name = "idx_tickets_status", columnList = "status")
        })
public class Ticket {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** Code de vérification non prédictible (UUID v4 + HMAC). Ne contient aucune donnée perso. */
    @Column(nullable = false, unique = true, length = 64)
    private String code;

    @Column(name = "booking_id", nullable = false)
    private Long bookingId;

    @Column(name = "ticket_tier_id", nullable = false)
    private Long ticketTierId;

    @Column(name = "customer_id", nullable = false)
    private Long customerId;

    /** Snapshot de l'eventId pour permettre la recherche sans appel cross-service. */
    @Column(name = "event_id", nullable = false)
    private Long eventId;

    /** Snapshot du titre de l'événement (utile pour l'affichage du billet). */
    @Column(name = "event_title", nullable = false, length = 150)
    private String eventTitle;

    /** Snapshot de la date de l'événement (pour expiration automatique). */
    @Column(name = "event_starts_at", nullable = false)
    private Instant eventStartsAt;

    /** Snapshot du nom du palier (Balcon, Orchestre, VIP...). */
    @Column(name = "tier_name", nullable = false, length = 80)
    private String tierName;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 15)
    private TicketStatus status = TicketStatus.ISSUED;

    @Column(name = "issued_at", nullable = false, updatable = false)
    private Instant issuedAt;

    @Column(name = "used_at")
    private Instant usedAt;

    @Column(name = "expired_at")
    private Instant expiredAt;

    @Column(name = "cancelled_at")
    private Instant cancelledAt;

    /** Optimistic locking — protège la transition ISSUED -> USED contre la concurrence. */
    @Version
    @Column(name = "version", nullable = false)
    private Long version = 0L;

    protected Ticket() {
    }

    public Ticket(String code, Long bookingId, Long ticketTierId, Long customerId, Long eventId,
                  String eventTitle, Instant eventStartsAt, String tierName) {
        this.code = code;
        this.bookingId = bookingId;
        this.ticketTierId = ticketTierId;
        this.customerId = customerId;
        this.eventId = eventId;
        this.eventTitle = eventTitle;
        this.eventStartsAt = eventStartsAt;
        this.tierName = tierName;
        this.issuedAt = Instant.now();
    }

    /** ISSUED -> USED (scan le jour de l'événement). */
    public void markUsed(Instant when) {
        if (status != TicketStatus.ISSUED) {
            throw new IllegalStateException("Only an ISSUED ticket can be marked USED, but it is " + status);
        }
        this.status = TicketStatus.USED;
        this.usedAt = when;
    }

    /** ISSUED -> EXPIRED (date de l'événement dépassée sans scan). */
    public void markExpired(Instant when) {
        if (status != TicketStatus.ISSUED) {
            throw new IllegalStateException("Only an ISSUED ticket can be expired, but it is " + status);
        }
        this.status = TicketStatus.EXPIRED;
        this.expiredAt = when;
    }

    /** ISSUED -> CANCELLED (suite à un remboursement de la réservation). */
    public void markCancelled(Instant when) {
        if (status != TicketStatus.ISSUED) {
            throw new IllegalStateException("Only an ISSUED ticket can be cancelled, but it is " + status);
        }
        this.status = TicketStatus.CANCELLED;
        this.cancelledAt = when;
    }

    // Getters
    public Long getId() { return id; }
    public String getCode() { return code; }
    public Long getBookingId() { return bookingId; }
    public Long getTicketTierId() { return ticketTierId; }
    public Long getCustomerId() { return customerId; }
    public Long getEventId() { return eventId; }
    public String getEventTitle() { return eventTitle; }
    public Instant getEventStartsAt() { return eventStartsAt; }
    public String getTierName() { return tierName; }
    public TicketStatus getStatus() { return status; }
    public Instant getIssuedAt() { return issuedAt; }
    public Instant getUsedAt() { return usedAt; }
    public Instant getExpiredAt() { return expiredAt; }
    public Instant getCancelledAt() { return cancelledAt; }
    public Long getVersion() { return version; }
}
