package com.ticketflow.aiassistantservice.entities;

/**
 * Statut d'un update Telegram reçu (CdC FR-019).
 *
 * - RECEIVED    : arrivé via webhook, en attente de traitement.
 * - PROCESSED   : traité et répondu (ou repli envoyé).
 * - DUPLICATE   : déjà vu (déduplication par update_id Telegram).
 * - UNSUPPORTED : type de message non géré (média non supporté, etc.) — réponse contrôlée.
 */
public enum TelegramUpdateStatus {
    RECEIVED,
    PROCESSED,
    DUPLICATE,
    UNSUPPORTED
}
