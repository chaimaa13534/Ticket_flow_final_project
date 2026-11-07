package com.ticketflow.eventservice.controllers;

import com.ticketflow.eventservice.dto.EventRequest;
import com.ticketflow.eventservice.dto.EventResponse;
import com.ticketflow.eventservice.services.EventService;
import jakarta.validation.Valid;
import java.math.BigDecimal;
import java.net.URI;
import java.time.Instant;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

/**
 * API REST Event — préfixe /api/v1 (CdC §7.2).
 *
 * Catalogue public (FR-001, FR-002, FR-003) :
 *  - GET /api/v1/events                  : liste paginée des PUBLISHED
 *  - GET /api/v1/events/search          : recherche multi-critères (texte, ville, catégorie, prix, dates)
 *  - GET /api/v1/events/{id}            : fiche détaillée (404 si non PUBLISHED sera géré à l'étape 4)
 *
 * Gestion événementielle (FR-004) — organisateur :
 *  - POST /api/v1/events                : créer (statut DRAFT)
 *  - PUT /api/v1/events/{id}            : modifier
 *  - POST /api/v1/events/{id}/publish   : DRAFT -> PUBLISHED
 *  - POST /api/v1/events/{id}/close     : PUBLISHED -> CLOSED
 *  - POST /api/v1/events/{id}/cancel    : -> CANCELLED
 *  - POST /api/v1/events/{id}/archive   : CLOSED -> ARCHIVED
 *
 * Sécurité : à l'étape 4, les mutations seront protégées par ROLE_ORGANIZER / ROLE_ADMIN.
 */
@RestController
@RequestMapping("/api/v1/events")
public class EventController {

    private final EventService service;

    public EventController(EventService service) { this.service = service; }

    // =========================================================================
    // Catalogue public (FR-001, FR-003)
    // =========================================================================

    @GetMapping
    public Page<EventResponse> listPublished(
            @PageableDefault(size = 20) Pageable pageable) {
        if (pageable.getPageSize() > 100) {
            pageable = PageRequest.of(pageable.getPageNumber(), 100,
                    pageable.getSortOr(Sort.by(Sort.Direction.ASC, "startsAt")));
        }
        return service.listPublished(pageable).map(EventResponse::from);
    }

    @GetMapping("/{id}")
    public EventResponse get(@PathVariable Long id) {
        // CdC FR-003 : 404 si l'événement n'est pas PUBLISHED — géré par Security à l'étape 4
        // Pour l'instant, on renvoie même les brouillons (pas encore de sécurité).
        return EventResponse.from(service.get(id));
    }

    /**
     * Recherche multi-critères (FR-002) :
     * GET /api/v1/events/search?text=concert&city=Paris&categoryId=2&minPrice=10&maxPrice=50
     */
    @GetMapping("/search")
    public Page<EventResponse> search(
            @RequestParam(required = false) String text,
            @RequestParam(required = false) String city,
            @RequestParam(required = false) Long categoryId,
            @RequestParam(required = false) Instant dateFrom,
            @RequestParam(required = false) Instant dateTo,
            @RequestParam(required = false) BigDecimal minPrice,
            @RequestParam(required = false) BigDecimal maxPrice,
            @PageableDefault(size = 20) Pageable pageable) {
        if (pageable.getPageSize() > 100) {
            pageable = PageRequest.of(pageable.getPageNumber(), 100, pageable.getSort());
        }
        return service.search(text, city, categoryId, dateFrom, dateTo, minPrice, maxPrice, pageable)
                .map(EventResponse::from);
    }

    // =========================================================================
    // Gestion événementielle (FR-004)
    // =========================================================================

    @PostMapping
    public ResponseEntity<EventResponse> create(@Valid @RequestBody EventRequest req) {
        EventResponse created = EventResponse.from(service.create(req));
        URI location = ServletUriComponentsBuilder.fromCurrentRequest()
                .path("/{id}").buildAndExpand(created.id()).toUri();
        return ResponseEntity.created(location).body(created);
    }

    @PutMapping("/{id}")
    public EventResponse update(@PathVariable Long id, @Valid @RequestBody EventRequest req) {
        return EventResponse.from(service.update(id, req));
    }

    @PostMapping("/{id}/publish")
    public EventResponse publish(@PathVariable Long id) {
        return EventResponse.from(service.publish(id));
    }

    @PostMapping("/{id}/close")
    public EventResponse close(@PathVariable Long id) {
        return EventResponse.from(service.close(id));
    }

    @PostMapping("/{id}/cancel")
    public EventResponse cancel(@PathVariable Long id) {
        return EventResponse.from(service.cancel(id));
    }

    @PostMapping("/{id}/archive")
    public EventResponse archive(@PathVariable Long id) {
        return EventResponse.from(service.archive(id));
    }

    /**
     * Liste des événements d'un organisateur — page "Mes événements" (FR-004).
     * À l'étape 4, organizerId sera extrait du JWT, pas fourni en query param.
     */
    @GetMapping("/by-organizer/{organizerId}")
    public Page<EventResponse> listByOrganizer(
            @PathVariable Long organizerId,
            @PageableDefault(size = 20) Pageable pageable) {
        if (pageable.getPageSize() > 100) {
            pageable = PageRequest.of(pageable.getPageNumber(), 100, pageable.getSort());
        }
        return service.listByOrganizer(organizerId, pageable).map(EventResponse::from);
    }
}
