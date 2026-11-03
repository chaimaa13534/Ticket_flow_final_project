package com.ticketflow.paymentservice.entities;

/**
 * Mode d'exécution d'un paiement (CdC FR-009).
 *
 * - SIMULATED : mode démo, le résultat est piloté par la config (success-rate).
 *               Permet de tester les scénarios succès/échec/annulation sans prestataire.
 * - PROVIDER  : adaptateur vers un prestataire réel (Stripe, etc.). Les webhooks
 *               sont authentifiés (HMAC) et idempotents.
 */
public enum PaymentMode {
    SIMULATED,
    PROVIDER
}
