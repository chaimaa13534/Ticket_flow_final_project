package com.ticketflow.eventservice.controllers;

import com.ticketflow.eventservice.dto.VenueRequest;
import com.ticketflow.eventservice.dto.VenueResponse;
import com.ticketflow.eventservice.services.VenueService;
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
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

@RestController
@RequestMapping("/api/v1/venues")
public class VenueController {

    private final VenueService service;

    public VenueController(VenueService service) { this.service = service; }

    @PostMapping
    public ResponseEntity<VenueResponse> create(@Valid @RequestBody VenueRequest req) {
        VenueResponse created = VenueResponse.from(service.create(req));
        URI location = ServletUriComponentsBuilder.fromCurrentRequest()
                .path("/{id}").buildAndExpand(created.id()).toUri();
        return ResponseEntity.created(location).body(created);
    }

    @GetMapping("/{id}")
    public VenueResponse get(@PathVariable Long id) {
        return VenueResponse.from(service.get(id));
    }

    @GetMapping
    public List<VenueResponse> list(@RequestParam(required = false) String city) {
        return (city == null || city.isBlank()
                ? service.findAll()
                : service.findByCity(city)
        ).stream().map(VenueResponse::from).toList();
    }

    @PutMapping("/{id}")
    public VenueResponse update(@PathVariable Long id, @Valid @RequestBody VenueRequest req) {
        return VenueResponse.from(service.update(id, req));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        service.delete(id);
        return ResponseEntity.noContent().build();
    }
}