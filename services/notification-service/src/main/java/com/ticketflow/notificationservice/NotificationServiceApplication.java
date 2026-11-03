package com.ticketflow.notificationservice;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;

/**
 * Notification Service — envoi transactionnel et informatif.
 *
 * Responsabilités (CdC §6.1, FR-014) :
 *  - Consommer les événements Kafka (BookingCreated, BookingConfirmed, TicketIssued, etc.).
 *  - Adapter par canal : e-mail (SMTP), Telegram (via ai-assistant-service), webhook externe.
 *  - Déduplication : une notification avec le même messageId n'est envoyée qu'une fois.
 *  - Reprises : en cas d'échec du canal, tentative avec backoff exponentiel puis DLT.
 *  - Préférences utilisateur et désabonnement respectés.
 *
 * Règle d'or (CdC §3.8) : un échec de canal n'annule ni une réservation ni un paiement.
 */
@SpringBootApplication
@EnableDiscoveryClient
public class NotificationServiceApplication {

    public static void main(String[] args) {
        SpringApplication.run(NotificationServiceApplication.class, args);
    }
}
