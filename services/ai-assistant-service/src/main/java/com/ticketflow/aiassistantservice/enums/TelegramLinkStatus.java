package com.ticketflow.aiassistantservice.enums;

/**
 * Statut d'une liaison Telegram (vue locale côté ai-assistant-service).
 *
 * Mêmes valeurs que customer-service.TelegramLinkStatus (pas de partage de code
 * pour préserver l'autonomie des services), synchronisées via événements Kafka.
 *
 * CdC FR-017 : Telegram seul ne prouve jamais l'identité métier.
 */
public enum TelegramLinkStatus {
    VERIFIED,
    REVOKED
}
