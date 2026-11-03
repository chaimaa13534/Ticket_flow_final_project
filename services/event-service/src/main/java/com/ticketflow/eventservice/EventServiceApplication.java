package com.ticketflow.eventservice;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;

/**
 * Event Service — catalogue d'événements.
 *
 * Responsabilités (CdC §6.1 et §7.1) :
 *  - CRUD événements, lieux, catégories, tarifs (TicketTier).
 *  - Cycle de vie : DRAFT -> PUBLISHED -> CLOSED -> ARCHIVED.
 *  - Filtrage public (recherche paginée par texte/date/lieu/catégorie/prix).
 *  - Aucune donnée de brouillon exposée publiquement (FR-003).
 *
 * Propriétaire de la base PostgreSQL ticketflow_event.
 */
@SpringBootApplication
@EnableDiscoveryClient
public class EventServiceApplication {

    public static void main(String[] args) {
        SpringApplication.run(EventServiceApplication.class, args);
    }
}
