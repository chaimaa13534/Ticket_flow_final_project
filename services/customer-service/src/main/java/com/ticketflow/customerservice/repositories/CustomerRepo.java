package com.ticketflow.customerservice.repositories;

import com.ticketflow.customerservice.entities.Customer;
import com.ticketflow.customerservice.entities.CustomerStatus;
import java.util.List;
import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface CustomerRepo extends JpaRepository<Customer, Long> {

    /** Recherche par email (insensible à la casse — l'email est normalisé en minuscules avant persist). */
    Optional<Customer> findByEmail(String email);

    /** Recherche par identifiant Keycloak (sub du JWT). */
    Optional<Customer> findByKeycloakId(String keycloakId);

    boolean existsByEmail(String email);

    boolean existsByKeycloakId(String keycloakId);

    /** Liste paginée filtrée par statut — utilisé par l'admin (FR-020). */
    Page<Customer> findByStatus(CustomerStatus status, Pageable pageable);

    /** Recherche plein texte par nom — utilisée par l'admin pour le support. */
    List<Customer> findByLastNameContainingIgnoreCaseOrFirstNameContainingIgnoreCase(
            String lastName, String firstName);
}
