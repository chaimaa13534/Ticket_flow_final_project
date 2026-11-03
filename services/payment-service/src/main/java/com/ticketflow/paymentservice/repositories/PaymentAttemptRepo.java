package com.ticketflow.paymentservice.repositories;

import com.ticketflow.paymentservice.entities.PaymentAttempt;
import com.ticketflow.paymentservice.entities.PaymentStatus;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface PaymentAttemptRepo extends JpaRepository<PaymentAttempt, Long> {

    Optional<PaymentAttempt> findByIdempotencyKeyAndCustomerId(String idempotencyKey, Long customerId);

    /**
     * Une réservation peut avoir PLUSIEURS tentatives de paiement (par ex. un premier échec
     * puis une retry réussie). On renvoie donc une List triée par date, pas un Optional.
     */
    List<PaymentAttempt> findByBookingIdOrderByCreatedAtDesc(Long bookingId);

    List<PaymentAttempt> findByCustomerIdOrderByCreatedAtDesc(Long customerId);

    List<PaymentAttempt> findByStatus(PaymentStatus status);
}
