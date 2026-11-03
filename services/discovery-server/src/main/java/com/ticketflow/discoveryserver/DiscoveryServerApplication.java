package com.ticketflow.discoveryserver;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.netflix.eureka.server.EnableEurekaServer;

/**
 * Serveur Eureka : registre de services.
 *
 * Tous les microservices métier s'y enregistrent au démarrage (spring.cloud.eureka.client.enabled=true)
 * et s'interrogent par nom logique ("event-service", "booking-service"…). L'API Gateway
 * et les clients OpenFeign utilisent ce registre pour résoudre les URLs.
 *
 * Le serveur lui-même ne s'enregistre PAS (register-with-eureka=false, fetch-registry=false) :
 * c'est LE registre, il n'a personne à s'enregistrer.
 */
@SpringBootApplication
@EnableEurekaServer
public class DiscoveryServerApplication {

    public static void main(String[] args) {
        SpringApplication.run(DiscoveryServerApplication.class, args);
    }
}
