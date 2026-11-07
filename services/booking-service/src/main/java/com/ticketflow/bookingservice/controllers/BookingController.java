package com.ticketflow.bookingservice.controllers;

import com.ticketflow.bookingservice.dto.BookingRequest;
import com.ticketflow.bookingservice.dto.BookingResponse;
import com.ticketflow.bookingservice.entities.Booking;
import com.ticketflow.bookingservice.services.BookingService;
import jakarta.validation.Valid;
import java.net.URI;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

/**
 * API REST Booking — préfixe /api/v1 (CdC §7.2).
 *
 * Endpoints :
 *  - POST /api/v1/bookings                  : créer (FR-007, FR-008)
 *  - GET /api/v1/bookings/{id}              : consulter (FR-013)
 *  - GET /api/v1/bookings                   : lister (admin ou filtré par client)
 *  - POST /api/v1/bookings/{id}/cancel      : annuler (FR-012)
 *
 * Routes spéciales gérées par la gateway :
 *  - GET /api/v1/me/bookings                 : mes réservations (customerId extrait du JWT)
 *
 * En-têtes :
 *  - Idempotency-Key (optionnel) : pour rendre la création idempotente (FR-008)
 *  - X-Correlation-Id (optionnel) : propagé par le gateway
 */
@RestController
@RequestMapping("/api/v1/bookings")
public class BookingController {

    private final BookingService service;

    public BookingController(BookingService service) {
        this.service = service;
    }

    @PostMapping
    public ResponseEntity<BookingResponse> create(
            @Valid @RequestBody BookingRequest req,
            @RequestHeader(value = "Idempotency-Key", required = false) String idempotencyKey) {
        Booking created = service.book(req, idempotencyKey);
        BookingResponse response = BookingResponse.from(created, service.getLines(created.getId()));
        URI location = ServletUriComponentsBuilder.fromCurrentRequest()
                .path("/{id}").buildAndExpand(created.getId()).toUri();
        return ResponseEntity.created(location).body(response);
    }

    @GetMapping("/{id}")
    public BookingResponse get(@PathVariable Long id) {
        Booking booking = service.get(id);
        return BookingResponse.from(booking, service.getLines(id));
    }

    @GetMapping
    public Page<BookingResponse> list(
            @RequestParam(required = false) Long customerId,
            @PageableDefault(size = 20) Pageable pageable) {
        if (pageable.getPageSize() > 100) {
            pageable = PageRequest.of(pageable.getPageNumber(), 100, pageable.getSort());
        }
        if (customerId != null) {
            return service.listByCustomer(customerId, pageable)
                    .map(b -> BookingResponse.from(b, service.getLines(b.getId())));
        }
        return service.listAll(pageable)
                .map(b -> BookingResponse.from(b, service.getLines(b.getId())));
    }

    @PostMapping("/{id}/cancel")
    public BookingResponse cancel(@PathVariable Long id) {
        Booking booking = service.cancel(id);
        return BookingResponse.from(booking, service.getLines(id));
    }

    /**
     * Confirmer une réservation (FR-010) — appelée par le payment-service après un paiement réussi.
     */
    @PostMapping("/{id}/confirm")
    public BookingResponse confirm(@PathVariable Long id) {
        Booking booking = service.confirm(id);
        return BookingResponse.from(booking, service.getLines(id));
    }
}
