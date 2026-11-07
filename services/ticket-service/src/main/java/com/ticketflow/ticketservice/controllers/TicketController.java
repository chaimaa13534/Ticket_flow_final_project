package com.ticketflow.ticketservice.controllers;

import com.ticketflow.ticketservice.dto.TicketResponse;
import com.ticketflow.ticketservice.entities.Ticket;
import com.ticketflow.ticketservice.services.TicketService;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * API REST Ticket — préfixe /api/v1 (CdC §7.2).
 *
 * Endpoints :
 *  - POST /api/v1/tickets/issue/{bookingId} : émettre les billets (FR-011)
 *  - GET /api/v1/tickets/{id}               : consulter par ID
 *  - GET /api/v1/tickets/by-code/{code}     : vérifier par code (FR-011)
 *  - GET /api/v1/tickets/by-booking/{id}    : billets d'une réservation
 *  - GET /api/v1/tickets/by-customer/{id}   : billets d'un client (FR-013)
 *  - POST /api/v1/tickets/{id}/use          : marquer comme utilisé (scan)
 */
@RestController
@RequestMapping("/api/v1/tickets")
public class TicketController {

    private final TicketService service;

    public TicketController(TicketService service) {
        this.service = service;
    }

    @PostMapping("/issue/{bookingId}")
    public ResponseEntity<java.util.List<TicketResponse>> issue(@PathVariable Long bookingId) {
        java.util.List<Ticket> tickets = service.issueTickets(bookingId);
        return ResponseEntity.ok(tickets.stream().map(TicketResponse::from).toList());
    }

    @GetMapping("/{id}")
    public TicketResponse get(@PathVariable Long id) {
        return TicketResponse.from(service.get(id));
    }

    @GetMapping("/by-code/{code}")
    public TicketResponse getByCode(@PathVariable String code) {
        return TicketResponse.from(service.getByCode(code));
    }

    @GetMapping("/by-booking/{bookingId}")
    public java.util.List<TicketResponse> listByBooking(@PathVariable Long bookingId) {
        return service.listByBooking(bookingId).stream().map(TicketResponse::from).toList();
    }

    @GetMapping("/by-customer/{customerId}")
    public Page<TicketResponse> listByCustomer(
            @PathVariable Long customerId,
            @PageableDefault(size = 20) Pageable pageable) {
        if (pageable.getPageSize() > 100) {
            pageable = PageRequest.of(pageable.getPageNumber(), 100, pageable.getSort());
        }
        return service.listByCustomer(customerId, pageable).map(TicketResponse::from);
    }

    @PostMapping("/{id}/use")
    public TicketResponse markUsed(@PathVariable Long id) {
        return TicketResponse.from(service.markUsed(id));
    }
}
