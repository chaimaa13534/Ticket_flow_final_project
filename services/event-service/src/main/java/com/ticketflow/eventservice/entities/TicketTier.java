package com.ticketflow.eventservice.entities;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.Version;
import java.math.BigDecimal;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * Palier de tarif pour un événement (CdC §7.1 : TicketTier).
 *
 * Exemples : "Balcon", "Orchestre", "VIP". Un événement peut avoir plusieurs paliers.
 *
 * IMPORTANT (CdC §7.1, écriture croisée interdite) :
 *  - {@code quantityAvailable} est le total offert par ce palier. Source de vérité côté
 *    event-service.
 *  - Le COMPTE des places vendues n'est PAS stocké ici : il est géré par booking-service
 *    dans sa propre base (table tier_inventory par ticket_tier_id). event-service ne sait
 *    jamais combien de places ont été vendues — c'est booking-service qui détient cette info.
 *
 * {@code @Version} active l'optimistic locking : deux modifications concurrentes du palier
 * (par l'organisateur) ne peuvent pas écraser la capacité silencieusement.
 *
 * {@code @ManyToOne} est LAZY : éviter de charger l'Event complet à chaque lecture du palier.
 */
@Entity
@Table(name = "ticket_tiers",
        indexes = {
                @Index(name = "idx_tiers_event", columnList = "event_id"),
                @Index(name = "uk_tiers_event_name", columnList = "event_id, name", unique = true)
        })
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class TicketTier {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(optional = false, fetch = FetchType.LAZY)
    @JoinColumn(name = "event_id", nullable = false)
    private Event event;

    @Column(nullable = false, length = 80)
    private String name;

    @Column(nullable = false, precision = 10, scale = 2)
    private BigDecimal price;

    /** Quantité totale disponible pour ce palier (source de vérité event-service). */
    @Column(name = "quantity_available", nullable = false)
    private int quantityAvailable;

    /** Optimistic locking : protège les modifications de capacité par l'organisateur. */
    @Version
    @Column(name = "version", nullable = false)
    private Long version = 0L;

    public TicketTier(Event event, String name, BigDecimal price, int quantityAvailable) {
        this.event = event;
        this.name = name;
        this.price = price;
        this.quantityAvailable = quantityAvailable;
    }

    public void update(String name, BigDecimal price, int quantityAvailable) {
        this.name = name;
        this.price = price;
        this.quantityAvailable = quantityAvailable;
    }
}
