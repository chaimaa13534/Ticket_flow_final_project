package com.ticketflow.customerservice;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;

/**
 * Customer Service — compte client, profil, rôles, liaison Telegram.
 *
 * Responsabilités (CdC §6.1) :
 *  - Inscription / connexion / profil d'un client.
 *  - Application des règles d'accès et de confidentialité.
 *  - Liaison Telegram initiée depuis une session authentifiée (code unique à usage unique).
 *
 * Ce service est propriétaire de sa base PostgreSQL (ticketflow_customer).
 * Il ne joint JAMAIS sa base avec celle d'un autre service.
 *
 * Authentification : le JWT est émis par Keycloak ; ce service le valide en tant que
 * Resource Server OIDC. Les endpoints distinguent les rôles ROLE_CLIENT, ROLE_ORGANIZER,
 * ROLE_ADMIN via les scopes du token.
 */
@SpringBootApplication
@EnableDiscoveryClient
public class CustomerServiceApplication {

    public static void main(String[] args) {
        SpringApplication.run(CustomerServiceApplication.class, args);
    }
}
