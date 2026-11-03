package com.ticketflow.paymentservice;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;
import org.springframework.cloud.openfeign.EnableFeignClients;

/**
 * Payment Service — paiement simulé et adaptateur prestataire.
 *
 * Responsabilités (CdC §6.1, FR-009) :
 *  - Mode simulé : succès/échec/annulation pilotables pour la démo (profil "demo").
 *  - Adaptateur prestataire : interface unifiée vers un futur fournisseur (Stripe, etc.).
 *  - Callbacks webhooks authentifiés (signature HMAC) et idempotents (clé = référence externe).
 *  - Publication Kafka : PaymentSucceeded / PaymentFailed pour le booking-service.
 *
 * Aucune carte bancaire stockée : uniquement une référence externe + statut.
 */
@SpringBootApplication
@EnableDiscoveryClient
@EnableFeignClients
public class PaymentServiceApplication {

    public static void main(String[] args) {
        SpringApplication.run(PaymentServiceApplication.class, args);
    }
}
