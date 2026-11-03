package com.ticketflow.customerservice.entities;

/**
 * Statut du compte client (CdC FR-005 : fermeture de compte).
 *
 * - ACTIVE     : compte opérationnel, l'utilisateur peut se connecter et réserver.
 * - SUSPENDED  : compte suspendu par un admin (audit / fraude), login refusé.
 * - CLOSED     : compte fermé à la demande de l'utilisateur, données conservées
 *                pendant {@code retention-days-after-closure} puis anonymisées.
 */
public enum CustomerStatus {
    ACTIVE,
    SUSPENDED,
    CLOSED
}