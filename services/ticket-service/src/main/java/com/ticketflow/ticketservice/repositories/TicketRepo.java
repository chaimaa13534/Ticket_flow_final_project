package com.ticketflow.ticketservice.repositories;

import com.ticketflow.ticketservice.entities.Ticket;
import com.ticketflow.ticketservice.entities.TicketStatus;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface TicketRepo extends JpaRepository<Ticket, Long> {

    /** Recherche par code de vérification (FR-011 : vérification de statut). */
    Optional<Ticket> findByCode(String code);

    /** Tous les billets d'une réservation (FR-013 : espace client). */
    List<Ticket> findByBookingId(Long bookingId);

    /** Tous les billets d'un client (FR-013 : jamais ceux d'autrui). */
    Page<Ticket> findByCustomerIdOrderByIssuedAtDesc(Long customerId, Pageable pageable);

    /** Sélection des billets ISSUED dont l'événement est dépassé (cron d'expiration). */
    List<Ticket> findByStatusAndEventStartsAtBefore(TicketStatus status, Instant when);
}