package com.ticketflow.eventservice.entities;

import com.ticketflow.eventservice.exceptions.InvalidEventStateException;
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
import java.math.BigDecimal;
import java.time.Instant;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * Événement billetterie (CdC §7.1, FR-001 à FR-004).
 *
 * Règles métier (CdC §3.6 et FR-004) :
 *  - Un événement commence toujours en DRAFT.
 *  - Une fois PUBLISHED, la capacité ne peut plus diminuer (les ventes ont peut-être commencé).
 *  - Une fois CLOSED, ARCHIVED ou CANCELLED : plus AUCUNE modification n'est autorisée.
 *  - {@code organizerId} référence un customer_id du customer-service (pas de FK cross-bases,
 *    CdC §7.1 dernière phrase).
 *
 * Le {@code basePrice} est le tarif d'entrée "from" affiché dans le catalogue public.
 * Les tarifs réels par catégorie de billet sont dans {@link TicketTier}.
 *
 * {@code @ManyToOne} est LAZY : on ne charge le venue et la catégorie que si nécessaire,
 * jamais par défaut (évite N+1 sur le catalogue public).
 */
@Entity
@Table(name = "events",
        indexes = {
                @Index(name = "idx_events_status", columnList = "status"),
                @Index(name = "idx_events_starts_at", columnList = "starts_at"),
                @Index(name = "idx_events_venue", columnList = "venue_id"),
                @Index(name = "idx_events_category", columnList = "category_id"),
                @Index(name = "idx_events_organizer", columnList = "organizer_id")
        })
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Event {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 150)
    private String title;

    @Column(length = 2000)
    private String description;

    @ManyToOne(optional = false, fetch = FetchType.LAZY)
    @JoinColumn(name = "venue_id", nullable = false)
    private Venue venue;

    @ManyToOne(optional = false, fetch = FetchType.LAZY)
    @JoinColumn(name = "category_id", nullable = false)
    private Category category;

    /** Identifiant client de l'organisateur (référence customer.id côté customer-service). */
    @Column(name = "organizer_id", nullable = false)
    private Long organizerId;

    @Column(name = "starts_at", nullable = false)
    private Instant startsAt;

    @Column(name = "ends_at")
    private Instant endsAt;

    /** Tarif "from" affiché dans le catalogue public (FR-001). */
    @Column(name = "base_price", nullable = false, precision = 10, scale = 2)
    private BigDecimal basePrice;

    @Column(nullable = false, length = 3)
    private String currency = "MAD";

    @Column(nullable = false)
    private int capacity;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private EventStatus status = EventStatus.DRAFT;

    @Column(name = "published_at")
    private Instant publishedAt;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt = Instant.now();

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt = Instant.now();

    /** Un nouvel événement commence toujours en DRAFT. */
    public Event(String title, String description, Venue venue, Category category, Long organizerId,
                 Instant startsAt, Instant endsAt, BigDecimal basePrice, int capacity) {
        validateDates(startsAt, endsAt);
        this.title = title;
        this.description = description;
        this.venue = venue;
        this.category = category;
        this.organizerId = organizerId;
        this.startsAt = startsAt;
        this.endsAt = endsAt;
        this.basePrice = basePrice;
        this.capacity = capacity;
    }

    /**
     * Mise à jour — transitions autorisées selon le statut courant.
     * - CLOSED, ARCHIVED, CANCELLED : aucune modification n'est autorisée.
     * - PUBLISHED : la capacité ne peut qu'augmenter (les ventes ont peut-être commencé).
     *
     * Règle : ends_at (si non null) doit rester postérieur à starts_at.
     */
    public void update(String title, String description, Venue venue, Category category,
                       Instant startsAt, Instant endsAt, BigDecimal basePrice, int capacity) {
        if (status == EventStatus.CLOSED
                || status == EventStatus.ARCHIVED
                || status == EventStatus.CANCELLED) {
            throw new InvalidEventStateException("A " + status + " event cannot be modified");
        }
        if (status == EventStatus.PUBLISHED && capacity < this.capacity) {
            throw new InvalidEventStateException(
                    "The capacity of a published event cannot be lowered ("
                            + this.capacity + " -> " + capacity + ")");
        }
        validateDates(startsAt, endsAt);
        this.title = title;
        this.description = description;
        this.venue = venue;
        this.category = category;
        this.startsAt = startsAt;
        this.endsAt = endsAt;
        this.basePrice = basePrice;
        this.capacity = capacity;
        this.updatedAt = Instant.now();
    }

    private void validateDates(Instant startsAt, Instant endsAt) {
        if (endsAt != null && !endsAt.isAfter(startsAt)) {
            throw new InvalidEventStateException(
                    "ends_at must be strictly after starts_at");
        }
    }

    /** DRAFT -> PUBLISHED. */
    public void publish() {
        if (status != EventStatus.DRAFT) {
            throw new InvalidEventStateException(
                    "Only a DRAFT event can be published, but it is " + status);
        }
        this.status = EventStatus.PUBLISHED;
        this.publishedAt = Instant.now();
        this.updatedAt = Instant.now();
    }

    /** PUBLISHED -> CLOSED. */
    public void close() {
        if (status != EventStatus.PUBLISHED) {
            throw new InvalidEventStateException(
                    "Only a PUBLISHED event can be closed, but it is " + status);
        }
        this.status = EventStatus.CLOSED;
        this.updatedAt = Instant.now();
    }

    /** PUBLISHED -> CANCELLED (et non DRAFT -> CANCELLED pour laisser les brouillons intacts). */
    public void cancel() {
        if (status != EventStatus.PUBLISHED && status != EventStatus.CLOSED) {
            throw new InvalidEventStateException(
                    "Only a PUBLISHED or CLOSED event can be cancelled, but it is " + status);
        }
        this.status = EventStatus.CANCELLED;
        this.updatedAt = Instant.now();
    }

    /** CLOSED -> ARCHIVED (masqué du catalogue public mais conservé pour audit). */
    public void archive() {
        if (status != EventStatus.CLOSED) {
            throw new InvalidEventStateException(
                    "Only a CLOSED event can be archived, but it is " + status);
        }
        this.status = EventStatus.ARCHIVED;
        this.updatedAt = Instant.now();
    }
}