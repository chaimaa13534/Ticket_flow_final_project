package com.ticketflow.customerservice.enums;

/**
 * Statut d'une liaison Telegram (CdC FR-017).
 *
 * Cycle de vie :
 * 1. PENDING   : code unique généré côté TicketFlow, à usage unique et durée courte.
 *                L'utilisateur doit le valider depuis Telegram.
 * 2. VERIFIED  : liaison confirmée — l'assistant peut consulter les réservations du client lié.
 * 3. REVOKED   : liaison révoquée par l'utilisateur ou un admin (à tout moment).
 * 4. EXPIRED   : code non validé dans le TTL — ne peut plus être utilisé.
 *
 * RÈGLE D'OR (CdC §2.3) : Telegram seul ne prouve jamais l'identité métier.
 *
 * RÉ-LIAISON (FR-017) : après REVOKED ou EXPIRED, l'utilisateur peut initier une NOUVELLE
 * liaison. L'unicité est donc partielle : seule une liaison PENDING ou VERIFIED est unique
 * par client (et par telegram_user_id VERIFIED). Voir la migration Flyway V001.
 */
public enum TelegramLinkStatus {
    PENDING,
    VERIFIED,
    REVOKED,
    EXPIRED
}