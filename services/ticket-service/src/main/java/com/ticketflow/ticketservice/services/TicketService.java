package com.ticketflow.ticketservice.services;

import com.ticketflow.ticketservice.clients.BookingClient;
import com.ticketflow.ticketservice.clients.BookingClient.BookingView;
import com.ticketflow.ticketservice.entities.Ticket;
import com.ticketflow.ticketservice.entities.TicketStatus;
import com.ticketflow.ticketservice.exceptions.ResourceNotFoundException;
import com.ticketflow.ticketservice.repositories.TicketRepo;
import java.time.Clock;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Logique métier des billets (CdC FR-011).
 *
 * RÔLE :
 *  - Émettre des billets uniques après confirmation de réservation (code UUID non prédictible).
 *  - Consulter un billet par son code de vérification.
 *  - Lister les billets d'un client (FR-013 : espace client).
 *  - Marquer un billet comme USED (scan le jour de l'événement).
 *  - Marquer un billet comme EXPIRED (date événement dépassée, via cron).
 *
 * Le code de billet ne contient AUCUNE donnée personnelle (CdC §3.5).
 */
@Service
@Transactional
public class TicketService {

    private final TicketRepo repo;
    private final BookingClient bookingClient;
    private final Clock clock;

    @Value("${ticketflow.ticket.code-signing-key:dev-ticket-signing-key-change-me}")
    private String codeSigningKey;

    public TicketService(TicketRepo repo, BookingClient bookingClient, Clock clock) {
        this.repo = repo;
        this.bookingClient = bookingClient;
        this.clock = clock;
    }

    /**
     * Émet des billets pour une réservation confirmée (FR-011).
     * Appelé manuellement via API (ou via Kafka à l'étape 5).
     */
    public List<Ticket> issueTickets(Long bookingId) {
        // 1. Vérifier via Feign que la réservation est CONFIRMED
        BookingView booking = fetchBooking(bookingId);
        if (!"CONFIRMED".equals(booking.status())) {
            throw new IllegalStateException(
                    "Cannot issue tickets for a booking with status " + booking.status());
        }

        // 2. Vérifier qu'on n'a pas déjà émis des billets pour cette réservation (anti-rejeu)
        List<Ticket> existing = repo.findByBookingId(bookingId);
        if (!existing.isEmpty()) {
            return existing; // Idempotence : on renvoie les billets existants
        }

        // 3. Émettre les billets
        // En vraie vie, on récupérerait les lignes de réservation via Feign
        // Ici on crée un seul billet pour la démo
        String code = generateCode();
        Ticket ticket = new Ticket(
                code,
                bookingId,
                1L, // ticketTierId (snapshot — en vraie vie récupéré via Feign)
                booking.customerId(),
                booking.eventId(),
                booking.eventTitle(),
                Instant.now().plusSeconds(86400 * 30), // eventStartsAt (snapshot)
                "Standard" // tierName (snapshot)
        );
        return List.of(repo.save(ticket));
    }

    @Transactional(readOnly = true)
    public Ticket getByCode(String code) {
        return repo.findByCode(code)
                .orElseThrow(() -> new ResourceNotFoundException("Ticket", code));
    }

    @Transactional(readOnly = true)
    public Ticket get(Long id) {
        return repo.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Ticket", String.valueOf(id)));
    }

    @Transactional(readOnly = true)
    public List<Ticket> listByBooking(Long bookingId) {
        return repo.findByBookingId(bookingId);
    }

    @Transactional(readOnly = true)
    public org.springframework.data.domain.Page<Ticket> listByCustomer(
            Long customerId, org.springframework.data.domain.Pageable pageable) {
        return repo.findByCustomerIdOrderByIssuedAtDesc(customerId, pageable);
    }

    /** Marquer un billet comme USED (scan le jour de l'événement). */
    public Ticket markUsed(Long id) {
        Ticket ticket = get(id);
        ticket.markUsed(Instant.now(clock));
        return ticket;
    }

    /** Marquer un billet comme EXPIRED (cron d'expiration). */
    public Ticket markExpired(Long id) {
        Ticket ticket = get(id);
        ticket.markExpired(Instant.now(clock));
        return ticket;
    }

    /** Liste des billets ISSUED dont l'événement est dépassé (cron). */
    @Transactional(readOnly = true)
    public List<Ticket> findExpiredTickets() {
        return repo.findByStatusAndEventStartsAtBefore(TicketStatus.ISSUED, Instant.now(clock));
    }

    // --- Helpers ---

    private BookingView fetchBooking(Long bookingId) {
        try {
            return bookingClient.getBooking(bookingId);
        } catch (Exception e) {
            throw new ResourceNotFoundException("Booking", String.valueOf(bookingId));
        }
    }

    /**
     * Génère un code de vérification non prédictible (UUID v4 + hash).
     * Ne contient AUCUNE donnée personnelle (CdC §3.5).
     */
    private String generateCode() {
        String uuid = UUID.randomUUID().toString().replace("-", "");
        // En production, on ajouterait un HMAC-SHA256 avec codeSigningKey
        // Pour la démo, on utilise juste l'UUID
        return "TK-" + uuid.substring(0, 8).toUpperCase() + "-"
                + uuid.substring(8, 16).toUpperCase();
    }
}
