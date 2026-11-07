package com.ticketflow.eventservice.services;

import com.ticketflow.eventservice.dto.EventRequest;
import com.ticketflow.eventservice.entities.Category;
import com.ticketflow.eventservice.entities.Event;
import com.ticketflow.eventservice.entities.EventStatus;
import com.ticketflow.eventservice.entities.Venue;
import com.ticketflow.eventservice.exceptions.ResourceNotFoundException;
import com.ticketflow.eventservice.repositories.EventRepo;
import com.ticketflow.eventservice.specifications.EventSpecifications;
import java.math.BigDecimal;
import java.time.Instant;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Logique métier des événements (CdC §3.6, FR-001 à FR-004).
 *
 * RÈGLES :
 *  - Un événement commence toujours en DRAFT (jamais PUBLISHED à la création).
 *  - La recherche publique ne renvoie QUE les événements PUBLISHED (FR-001, FR-003).
 *  - Les transitions de statut sont validées par l'entité Event elle-même.
 *  - {@code organizerId} sera extrait du JWT à l'étape 4 (sub du rôle ORGANIZER).
 */
@Service
@Transactional
public class EventService {

    private final EventRepo repo;
    private final VenueService venueService;
    private final CategoryService categoryService;

    public EventService(EventRepo repo, VenueService venueService, CategoryService categoryService) {
        this.repo = repo;
        this.venueService = venueService;
        this.categoryService = categoryService;
    }

    public Event create(EventRequest req) {
        Venue venue = venueService.get(req.venueId());
        Category category = categoryService.get(req.categoryId());
        // Le statut est toujours DRAFT à la création — l'organisateur doit explicitement publier.
        Event event = new Event(req.title(), req.description(), venue, category,
                req.organizerId(), req.startsAt(), req.endsAt(),
                req.basePrice(), req.capacity());
        return repo.save(event);
    }

    @Transactional(readOnly = true)
    public Event get(Long id) {
        return repo.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Event", String.valueOf(id)));
    }

    /**
     * Catalogue public : événements PUBLISHED paginés (FR-001).
     */
    @Transactional(readOnly = true)
    public Page<Event> listPublished(Pageable pageable) {
        return repo.findByStatusOrderByStartsAtAsc(EventStatus.PUBLISHED, pageable);
    }

    /**
     * Recherche multi-critères (FR-002) — utilise Specification pour construire la requête
     * dynamiquement sans le pattern fragile `:param IS NULL OR ...`.
     */
    @Transactional(readOnly = true)
    public Page<Event> search(String text, String city, Long categoryId,
                              Instant dateFrom, Instant dateTo,
                              BigDecimal minPrice, BigDecimal maxPrice,
                              Pageable pageable) {
        Specification<Event> spec = EventSpecifications.withFilters(
                text, city, categoryId, dateFrom, dateTo, minPrice, maxPrice);
        return repo.findAll(spec, pageable);
    }

    /**
     * Événements d'un organisateur — pour la page "Mes événements" (FR-004).
     * Sera protégé par ROLE_ORGANIZER à l'étape 4 (organizerId extrait du JWT).
     */
    @Transactional(readOnly = true)
    public Page<Event> listByOrganizer(Long organizerId, Pageable pageable) {
        return repo.findByOrganizerIdOrderByCreatedAtDesc(organizerId, pageable);
    }

    public Event update(Long id, EventRequest req) {
        Event event = get(id);
        Venue venue = venueService.get(req.venueId());
        Category category = categoryService.get(req.categoryId());
        // L'entité Event valide elle-même les transitions d'état (InvalidEventStateException)
        event.update(req.title(), req.description(), venue, category,
                req.startsAt(), req.endsAt(), req.basePrice(), req.capacity());
        return event;
    }

    public Event publish(Long id) {
        Event event = get(id);
        event.publish();
        return event;
    }

    public Event close(Long id) {
        Event event = get(id);
        event.close();
        return event;
    }

    public Event cancel(Long id) {
        Event event = get(id);
        event.cancel();
        return event;
    }

    public Event archive(Long id) {
        Event event = get(id);
        event.archive();
        return event;
    }
}
