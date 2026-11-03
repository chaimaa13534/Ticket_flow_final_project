package com.ticketflow.bookingservice;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;
import org.springframework.cloud.openfeign.EnableFeignClients;

/**
 * Booking Service — réservations et inventaire.
 *
 * Responsabilités (CdC §6.1, §7.1, §3.3) :
 *  - Vérification atomique de la capacité (UPDATE conditionnel, pas de SELECT-then-UPDATE).
 *  - Snapshot du prix et du titre de l'événement au moment de la réservation.
 *  - TTL de la réservation (PENDING -> EXPIRED après N minutes sans paiement).
 *  - Idempotence : Idempotency-Key acceptée en header.
 *  - Transition : PENDING -> CONFIRMED (après paiement réussi) -> CANCELLED -> REFUNDED.
 *
 * Communication :
 *  - Synchrone vers event-service (OpenFeign + Resilience4J) pour valider PUBLISHED + capacité.
 *  - Synchrone vers customer-service pour vérifier l'identité.
 *  - Asynchrone (Kafka) : publie BookingCreated/Expired/Confirmed ; consomme PaymentSucceeded/Failed.
 */
@SpringBootApplication
@EnableDiscoveryClient
@EnableFeignClients
public class BookingServiceApplication {

    public static void main(String[] args) {
        SpringApplication.run(BookingServiceApplication.class, args);
    }
}
