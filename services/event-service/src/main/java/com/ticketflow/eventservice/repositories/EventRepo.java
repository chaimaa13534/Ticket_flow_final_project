package com.ticketflow.eventservice.repositories;

import com.ticketflow.eventservice.entities.Event;
import com.ticketflow.eventservice.entities.EventStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;

/**
 * Repository Event avec Specification Executor pour la recherche multi-critères (FR-002).
 *
 * On évite le pattern JPQL `(:param IS NULL OR ...)` qui pose problème avec PostgreSQL
 * quand le param est nul. À la place, on construit dynamiquement une Specification
 * côté service (étape 3a) en n'ajoutant que les critères réellement fournis.
 */
@Repository
public interface EventRepo extends JpaRepository<Event, Long>, JpaSpecificationExecutor<Event> {

    /**
     * Catalogue public paginé : événements PUBLISHED triés par date de début (FR-001).
     * Utilise un EntityGraph pour éviter N+1 sur venue et category.
     */
    @EntityGraph(attributePaths = {"venue", "category"})
    Page<Event> findByStatusOrderByStartsAtAsc(EventStatus status, Pageable pageable);

    /** Liste des événements d'un organisateur (FR-004 : gestion événementielle). */
    Page<Event> findByOrganizerIdOrderByCreatedAtDesc(Long organizerId, Pageable pageable);
}