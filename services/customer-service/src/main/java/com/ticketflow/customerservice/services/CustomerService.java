package com.ticketflow.customerservice.services;

import com.ticketflow.customerservice.dto.CustomerRequest;
import com.ticketflow.customerservice.entities.Customer;
import com.ticketflow.customerservice.exceptions.DuplicateEmailException;
import com.ticketflow.customerservice.exceptions.ResourceNotFoundException;
import com.ticketflow.customerservice.repositories.CustomerRepo;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Logique métier des comptes clients.
 *
 * RÈGLES :
 *  - {@code email} est normalisé en minuscules avant persistence (sinon deux emails
 *    avec casse différente pourraient contourner l'unicité).
 *  - La création depuis l'API publique ne permet PAS de définir le statut (toujours ACTIVE).
 *  - Le keycloakId sera injecté à l'étape 4 depuis le JWT Keycloak (sub).
 *
 * Note : suspend/reactivate/close seront protégés par ROLE_ADMIN à l'étape 4.
 */
@Service
@Transactional
public class CustomerService {

    private final CustomerRepo repo;

    public CustomerService(CustomerRepo repo) {
        this.repo = repo;
    }

    public Customer create(CustomerRequest request, String keycloakId) {
        String email = normalize(request.email());
        if (repo.existsByEmail(email)) {
            throw new DuplicateEmailException(email);
        }
        Customer customer = new Customer(
                keycloakId != null ? keycloakId : "pending-keycloak-" + System.nanoTime(),
                request.firstName(),
                request.lastName(),
                email,
                request.phone()
        );
        return repo.save(customer);
    }

    @Transactional(readOnly = true)
    public Customer get(Long id) {
        return repo.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Customer", String.valueOf(id)));
    }

    @Transactional(readOnly = true)
    public Customer getByEmail(String email) {
        return repo.findByEmail(normalize(email))
                .orElseThrow(() -> new ResourceNotFoundException("Customer", email));
    }

    @Transactional(readOnly = true)
    public Page<Customer> list(Pageable pageable) {
        return repo.findAll(pageable);
    }

    public Customer update(Long id, CustomerRequest request) {
        Customer customer = get(id);
        String email = normalize(request.email());
        if (!customer.getEmail().equals(email) && repo.existsByEmail(email)) {
            throw new DuplicateEmailException(email);
        }
        customer.update(request.firstName(), request.lastName(), email, request.phone());
        return customer;
    }

    public void delete(Long id) {
        Customer customer = get(id);
        repo.delete(customer);
    }

    /** Suspendre un compte — sera protégé par ROLE_ADMIN à l'étape 4. */
    public Customer suspend(Long id) {
        Customer customer = get(id);
        customer.suspend();
        return customer;
    }

    /** Réactiver un compte — sera protégé par ROLE_ADMIN à l'étape 4. */
    public Customer reactivate(Long id) {
        Customer customer = get(id);
        customer.reactivate();
        return customer;
    }

    /** Fermer un compte (à la demande du client) — CdC FR-005. */
    public Customer close(Long id) {
        Customer customer = get(id);
        customer.close();
        return customer;
    }

    private String normalize(String email) {
        return email == null ? null : email.trim().toLowerCase();
    }
}