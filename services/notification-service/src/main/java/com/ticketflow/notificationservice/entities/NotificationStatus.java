package com.ticketflow.notificationservice.entities;

/**
 * Statut d'une notification (CdC FR-014).
 *
 * - PENDING    : en file d'attente.
 * - SENT       : envoyée au canal sans erreur de transport.
 * - DELIVERED  : accusé de réception reçu (si canal l'expose).
 * - FAILED     : échec après toutes les reprises.
 * - SUPPRESSED : intentionnellement non envoyée (préférence désactivée, désabonnement).
 */
public enum NotificationStatus {
    PENDING,
    SENT,
    DELIVERED,
    FAILED,
    SUPPRESSED
}
