package com.ticketflow.notificationservice.entities;

/**
 * Canal d'envoi d'une notification (CdC §6.1, FR-014).
 *
 * - EMAIL     : adaptateur SMTP (mocké en démo).
 * - TELEGRAM  : délégué à ai-assistant-service (Bot Telegram).
 * - WEBHOOK   : webhook externe optionnel ( Slack, etc.).
 */
public enum NotificationChannel {
    EMAIL,
    TELEGRAM,
    WEBHOOK
}
