package com.ticketflow.bookingservice.entities;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.Version;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * Inventaire des places PAR PALIER DE TARIF (CdC §7.1 et FR-007, FR-008).
 *
 * IMPORTANT (correction architecturale) :
 *  - Clé primaire = {@code ticketTierId} (référence au palier côté event-service, pas de FK).
 *  - Pas d'inventaire par eventId : une réservation se fait par palier (CdC §3.3 et FR-007).
 *  - Pas de quantity_sold côté event-service : c'est ICI que le compte des ventes est tenu,
 *    dans la base du booking-service (pas d'écriture croisée, CdC §7.1).
 *
 * CONTRÔLE DE CONCURRENCE :
 *  - {@code version} active l'optimistic locking : deux threads ne peuvent pas réserver
 *    la même place simultanément.
 *  - Les méthodes {@code reserve} et {@code release} du repository utilisent un
 *    UPDATE conditionnel atomique (cf. TierInventoryRepo) qui renvoie 0 si la capacité
 *    est dépassée — c'est la garantie CdC §3.3 : "vérification atomique de la capacité".
 *
 *  Capacité disponible = capacity - held - sold (jamais négatif).
 */
@Entity
@Table(name = "tier_inventory")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class TierInventory {

    /** PK = ticketTierId (référence ticket_tier.id côté event-service, pas de FK cross-bases). */
    @Id
    private Long ticketTierId;

    /** Capacité totale du palier (snapshot copié depuis event-service à la première réservation). */
    @Column(nullable = false)
    private int capacity;

    /** Places en attente de paiement (réservations PENDING sur ce palier). */
    @Column(nullable = false)
    private int held = 0;

    /** Places confirmées (réservations CONFIRMED sur ce palier). */
    @Column(nullable = false)
    private int sold = 0;

    @Version
    @Column(name = "version", nullable = false)
    private Long version = 0L;

    /** Constructeur JPA-friendly (initialiser à la première réservation d'un palier). */
    public TierInventory(Long ticketTierId, int capacity) {
        this.ticketTierId = ticketTierId;
        this.capacity = capacity;
    }

    /** Capacité encore disponible pour de nouvelles réservations PENDING. */
    public int available() {
        return capacity - held - sold;
    }
}
