package com.ticketflow.eventservice.controllers;

import com.ticketflow.eventservice.dto.TicketTierRequest;
import com.ticketflow.eventservice.dto.TicketTierResponse;
import com.ticketflow.eventservice.services.TicketTierService;
import jakarta.validation.Valid;
import java.net.URI;
import java.util.List;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

@RestController
@RequestMapping("/api/v1/ticket-tiers")
public class TicketTierController {

    private final TicketTierService service;

    public TicketTierController(TicketTierService service) { this.service = service; }

    @PostMapping
    public ResponseEntity<TicketTierResponse> create(@Valid @RequestBody TicketTierRequest req) {
        TicketTierResponse created = TicketTierResponse.from(service.create(req));
        URI location = ServletUriComponentsBuilder.fromCurrentRequest()
                .path("/{id}").buildAndExpand(created.id()).toUri();
        return ResponseEntity.created(location).body(created);
    }

    @GetMapping("/{id}")
    public TicketTierResponse get(@PathVariable Long id) {
        return TicketTierResponse.from(service.get(id));
    }

    @GetMapping("/by-event/{eventId}")
    public List<TicketTierResponse> listByEvent(@PathVariable Long eventId) {
        return service.findByEvent(eventId).stream().map(TicketTierResponse::from).toList();
    }

    @PutMapping("/{id}")
    public TicketTierResponse update(@PathVariable Long id, @Valid @RequestBody TicketTierRequest req) {
        return TicketTierResponse.from(service.update(id, req));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        service.delete(id);
        return ResponseEntity.noContent().build();
    }
}
