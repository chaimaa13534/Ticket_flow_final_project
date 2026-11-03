package com.ticketflow.ticketservice;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;
import org.springframework.cloud.openfeign.EnableFeignClients;

/**
 * Ticket Service — émission et consultation des billets.
 *
 * Responsabilités (CdC §6.1, FR-011) :
 *  - Émission automatique d'un billet unique par ligne de commande après confirmation.
 *  - Code de contrôle NON prédictible (UUID v4 + HMAC) — n'expose aucune donnée personnelle.
 *  - Consultation par le client propriétaire uniquement (vérification d'identité via Feign).
 *  - Vérification de statut (valide / utilisé / expiré).
 *
 * Communication :
 *  - Consomme Kafka : BookingConfirmed -> émet les billets.
 *  - Publie Kafka : TicketIssued -> déclenche la notification.
 *  - Synchrone vers booking-service : vérifie que la réservation est CONFIRMED et appartient au client.
 */
@SpringBootApplication
@EnableDiscoveryClient
@EnableFeignClients
public class TicketServiceApplication {

    public static void main(String[] args) {
        SpringApplication.run(TicketServiceApplication.class, args);
    }
}
