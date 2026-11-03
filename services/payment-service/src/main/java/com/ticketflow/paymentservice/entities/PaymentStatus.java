package com.ticketflow.paymentservice.entities;

/**
 * Statut d'une tentative de paiement (CdC FR-009 et FR-012).
 *
 * Cycle :
 *   PENDING  ──success──▶  SUCCEEDED
 *   PENDING  ──fail───▶    FAILED
 *   PENDING  ──cancel──▶   CANCELLED
 *
 *   SUCCEEDED  ──refund (total)──▶  REFUNDED
 *   SUCCEEDED  ──refund (partiel)──▶  PARTIALLY_REFUNDED
 *
 * CdC §3.4 : "Une réservation n'est confirmée qu'après un état de paiement admissible"
 * = SUCCEEDED.
 */
public enum PaymentStatus {
    PENDING,
    SUCCEEDED,
    FAILED,
    CANCELLED,
    REFUNDED,
    PARTIALLY_REFUNDED
}