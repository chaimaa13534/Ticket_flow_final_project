package com.ticketflow.bookingservice.entities;

/**
 * Statut d'une réservation (CdC §3.3, §3.4, FR-007, FR-010, FR-012).
 *
 * Cycle de vie canonique :
 *
 *   PENDING  ──confirm──▶  CONFIRMED  ──cancel──▶  CANCELLED
 *     │                       │
 *     │                       └──refund──▶  REFUNDED
 *     │
 *     └──expire──▶  EXPIRED  (TTL dépassé sans paiement, FR-008)
 *
 * Règles métier :
 *  - PENDING   : créé après contrôle de capacité, en attente de paiement pendant TTL.
 *  - CONFIRMED : unique, survient après paiement admissible (FR-010).
 *  - EXPIRED   : TTL écoulé sans paiement, capacité restituée (FR-008).
 *  - CANCELLED : annulée par le client selon politique (FR-012).
 *  - REFUNDED  : remboursée suite à une annulation post-confirmation.
 */
public enum BookingStatus {
    PENDING,
    CONFIRMED,
    CANCELLED,
    EXPIRED,
    REFUNDED
}