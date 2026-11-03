package com.ticketflow.bookingservice.repositories;

import com.ticketflow.bookingservice.entities.TierInventory;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

/**
 * Repository d'inventaire PAR PALIER DE TARIF (CdC FR-007, FR-008).
 *
 * Les méthodes {@code reserve}, {@code release}, {@code confirm}, {@code refund}
 * implémentent la vérification atomique de la capacité imposée par le CdC FR-008
 * ("réservation atomique anti-concurrence").
 *
 * POURQUOI un UPDATE conditionnel plutôt qu'un SELECT-then-UPDATE :
 *  - Un SELECT-then-UPDATE laisse une fenêtre de temps où deux threads peuvent lire
 *    la même valeur de {@code held} et tous deux réussir —> surréservation.
 *  - Un UPDATE conditionnel rend l'opération atomique au niveau BDD : si
 *    {@code available - quantity >= 0} n'est pas vrai au moment de l'UPDATE, ligne renvoyée = 0
 *    et on lève BookingRuleException côté service.
 *
 * POURQUOI par ticketTierId et non par eventId :
 *  - Une réservation se fait sur un palier (TicketTier) précis, pas sur l'événement global
 *    (CdC §3.3 et FR-007 : "Le client choisit événement, catégorie et quantité").
 *  - event-service détient quantity_available du palier ; booking-service détient le compte
 *    des ventes (held + sold) — pas d'écriture croisée (CdC §7.1).
 *
 * NOTE sur @Modifying(clearAutomatically = true, flushAutomatically = true) :
 *  - clearAutomatically : vide le contexte de persistance après l'UPDATE, pour éviter
 *    que des entités chargées avant l'UPDATE soient renvoyées avec des valeurs périmées.
 *  - flushAutomatically : flush le contexte avant l'UPDATE, pour que les modifications
 *    en attente soient persistées avant le UPDATE conditionnel (sinon elles seraient perdues).
 */
@Repository
public interface TierInventoryRepo extends JpaRepository<TierInventory, Long> {

    /**
     * Réserve atomiquement {@code quantity} places d'un palier.
     *
     * @return 1 si la réservation a réussi, 0 si la capacité était insuffisante.
     */
    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("UPDATE TierInventory t SET t.held = t.held + :quantity "
            + "WHERE t.ticketTierId = :ticketTierId AND (t.capacity - t.held - t.sold) >= :quantity")
    int reserve(@Param("ticketTierId") Long ticketTierId, @Param("quantity") int quantity);

    /**
     * Libère {@code quantity} places (annulation ou expiration d'une réservation PENDING).
     */
    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("UPDATE TierInventory t SET t.held = t.held - :quantity "
            + "WHERE t.ticketTierId = :ticketTierId AND t.held >= :quantity")
    int release(@Param("ticketTierId") Long ticketTierId, @Param("quantity") int quantity);

    /**
     * Convertit {@code quantity} places de "held" à "sold" (confirmation après paiement, FR-010).
     * Atomique : ne peut pas descendre {@code held} en dessous de 0.
     */
    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("UPDATE TierInventory t SET t.held = t.held - :quantity, t.sold = t.sold + :quantity "
            + "WHERE t.ticketTierId = :ticketTierId AND t.held >= :quantity")
    int confirm(@Param("ticketTierId") Long ticketTierId, @Param("quantity") int quantity);

    /**
     * Remet {@code quantity} places "sold" dans la capacité disponible (remboursement, FR-012).
     */
    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("UPDATE TierInventory t SET t.sold = t.sold - :quantity "
            + "WHERE t.ticketTierId = :ticketTierId AND t.sold >= :quantity")
    int refund(@Param("ticketTierId") Long ticketTierId, @Param("quantity") int quantity);
}
