package com.ticketflow.customerservice.controllers;

import com.ticketflow.customerservice.dto.CustomerRequest;
import com.ticketflow.customerservice.dto.CustomerResponse;
import com.ticketflow.customerservice.services.CustomerService;
import jakarta.validation.Valid;
import java.net.URI;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
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

/**
 * API REST Customer — préfixe /api/v1 (CdC §7.2).
 *
 * Sécurité : NON encore activée (étape 4). Pour l'instant, endpoints ouverts pour les tests.
 * À l'étape 4 :
 *  - POST /api/v1/customers → public (signup)
 *  - GET /api/v1/customers → ROLE_ADMIN
 *  - GET/PUT /api/v1/customers/{id} → le client lui-même ou ROLE_ADMIN
 *  - DELETE, /suspend, /reactivate → ROLE_ADMIN
 *  - /close → le client lui-même
 */
@RestController
@RequestMapping("/api/v1/customers")
public class CustomerController {

    private final CustomerService service;

    public CustomerController(CustomerService service) {
        this.service = service;
    }

    @PostMapping
    public ResponseEntity<CustomerResponse> create(@Valid @RequestBody CustomerRequest request) {
        // keycloakId sera extrait du JWT à l'étape 4 — null pour l'instant.
        CustomerResponse created = CustomerResponse.from(service.create(request, null));
        URI location = ServletUriComponentsBuilder.fromCurrentRequest()
                .path("/{id}").buildAndExpand(created.id()).toUri();
        return ResponseEntity.created(location).body(created);
    }

    @GetMapping("/{id}")
    public CustomerResponse get(@PathVariable Long id) {
        return CustomerResponse.from(service.get(id));
    }

    @GetMapping
    public Page<CustomerResponse> list(@PageableDefault(size = 20) Pageable pageable) {
        // Limite hardcodée à 100 pour empêcher un client de demander size=10000
        if (pageable.getPageSize() > 100) {
            pageable = PageRequest.of(pageable.getPageNumber(), 100, pageable.getSort());
        }
        return service.list(pageable).map(CustomerResponse::from);
    }

    @PutMapping("/{id}")
    public CustomerResponse update(@PathVariable Long id, @Valid @RequestBody CustomerRequest request) {
        return CustomerResponse.from(service.update(id, request));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        service.delete(id);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/{id}/suspend")
    public CustomerResponse suspend(@PathVariable Long id) {
        return CustomerResponse.from(service.suspend(id));
    }

    @PostMapping("/{id}/reactivate")
    public CustomerResponse reactivate(@PathVariable Long id) {
        return CustomerResponse.from(service.reactivate(id));
    }

    @PostMapping("/{id}/close")
    public CustomerResponse close(@PathVariable Long id) {
        return CustomerResponse.from(service.close(id));
    }
}