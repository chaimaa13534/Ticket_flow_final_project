package com.ticketflow.ticketservice.entities;

/**
 * Statut d'un billet (CdC FR-011 : émission, consultation, vérification).
 *
 *   ISSUED  ──scan──▶  USED
 *     │
 *     └──expire (date événement dépassée)──▶  EXPIRED
 *
 *   ISSUED  ──cancel──▶  CANCELLED  (si réservation remboursée)
 *
 * {@code VALID} = émis et pas encore scanné, pas expiré.
 * Le statut VALID n'est PAS persisté : il est dérivé de ISSUED + dates.
 */
public enum TicketStatus {
    ISSUED,    // Émis après confirmation de paiement (FR-011)
    USED,      // Scanné / validé le jour de l'événement
    EXPIRED,   // Date de validité dépassée sans scan
    CANCELLED  // Annulé suite à un remboursement de la réservation
}
