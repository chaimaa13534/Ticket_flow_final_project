package com.ticketflow.apigateway;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;

/**
 * API Gateway — point d'entrée unique côté client.
 *
 * Responsabilités (conformément au cahier des charges §6.1) :
 *  - Exposer des routes explicites vers chaque microservice (pas de découverte automatique).
 *  - Valider le JWT émis par Keycloak avant de forwarder la requête.
 *  - Propager l'identité (sub, roles) sous forme de headers internes X-User-Id, X-User-Roles.
 *  - Gérer le CORS pour le frontend Angular.
 *  - Imposer un rate limiting par adresse IP.
 *  - Propager un identifiant de corrélation X-Correlation-Id (créé si absent).
 *
 * Le gateway NE fait aucun métier : il ne fait que router et filtrer.
 */
@SpringBootApplication
@EnableDiscoveryClient
public class ApiGatewayApplication {

    public static void main(String[] args) {
        SpringApplication.run(ApiGatewayApplication.class, args);
    }
}
