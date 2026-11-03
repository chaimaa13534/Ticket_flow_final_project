package com.ticketflow.bookingservice.repositories;

import com.ticketflow.bookingservice.entities.Booking;
import com.ticketflow.bookingservice.entities.BookingStatus;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface BookingRepo extends JpaRepository<Booking, Long> {

    Optional<Booking> findByBookingReference(String reference);

    // NOTE : la recherche par idempotency_key se fait via IdempotencyKeyRepo.findByCustomerIdAndIdempotencyKey()
    // qui renvoie l'IdempotencyKey contenant bookingId. Pas de duplication de la colonne dans bookings.

    /** Espace client : réservations d'un client, triées par date (FR-013). */
    Page<Booking> findByCustomerIdOrderByCreatedAtDesc(Long customerId, Pageable pageable);

    /** Toutes les réservations, optionnellement filtrées par client (admin). */
    List<Booking> findByCustomerIdOrderByCreatedAtDescIdDesc(Long customerId);

    List<Booking> findAllByOrderByCreatedAtDescIdDesc();

    /**
     * Sélection des réservations PENDING échues (cron de nettoyage, FR-008).
     *
     * IMPORTANT : la transition en EXPIRED et la libération des places associées
     * se fait côté service (BookingService) dans une transaction, en appelant
     * TierInventoryRepo.release() pour chaque ligne. Le bulk UPDATE précédent
     * (bulkExpirePending) libérait les places sans compensation — supprimé.
     */
    @Query("SELECT b FROM Booking b WHERE b.status = :status AND b.expiresAt < :now")
    List<Booking> findExpiredPending(@Param("status") BookingStatus status, @Param("now") Instant now);
}