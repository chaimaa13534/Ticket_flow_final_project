package com.ticketflow.paymentservice.services;

import com.ticketflow.paymentservice.clients.BookingClient;
import com.ticketflow.paymentservice.dto.PaymentRequest;
import com.ticketflow.paymentservice.entities.PaymentAttempt;
import com.ticketflow.paymentservice.entities.PaymentMode;
import com.ticketflow.paymentservice.entities.PaymentStatus;
import com.ticketflow.paymentservice.exceptions.ResourceNotFoundException;
import com.ticketflow.paymentservice.repositories.PaymentAttemptRepo;
import feign.FeignException;
import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Logique métier des paiements (CdC FR-009).
 *
 * Mode SIMULATED : le résultat (succès/échec) est piloté par la config (success-rate).
 * En cas de succès, on appelle le booking-service pour confirmer la réservation (FR-010).
 */
@Service
@Transactional
public class PaymentService {

    private final PaymentAttemptRepo repo;
    private final BookingClient bookingClient;
    private final Clock clock;

    @Value("${ticketflow.payment.mode:simulated}")
    private String paymentMode;

    @Value("${ticketflow.payment.success-rate:0.9}")
    private double successRate;

    public PaymentService(PaymentAttemptRepo repo, BookingClient bookingClient, Clock clock) {
        this.repo = repo;
        this.bookingClient = bookingClient;
        this.clock = clock;
    }

    /**
     * Simule un paiement pour une réservation (FR-009).
     */
    public PaymentAttempt simulatePayment(PaymentRequest req) {
        // 1. Créer la tentative de paiement (montant factice pour la démo)
        PaymentAttempt payment = new PaymentAttempt(
                req.bookingId(),
                req.customerId(),
                BigDecimal.valueOf(100.0), // En vraie vie, on récupérerait le montant via bookingClient
                PaymentMode.SIMULATED,
                null
        );
        payment = repo.save(payment);

        // 2. Simuler le résultat (succès aléatoire basé sur successRate)
        Instant now = Instant.now(clock);
        if (Math.random() < successRate) {
            // 3a. Succès
            payment.markSucceeded("SIMULATED-CHARGE-" + payment.getId(), now);

            // 4. Confirmer la réservation via Feign (FR-010)
            try {
                bookingClient.confirmBooking(req.bookingId());
            } catch (FeignException e) {
                // Si booking-service est down, on logge mais on ne rollback pas le paiement
                // (saga pattern serait géré à l'étape 5 avec Kafka)
            }
        } else {
            // 3b. Échec
            payment.markFailed("Simulated failure (random)", now);
        }

        return payment;
    }

    @Transactional(readOnly = true)
    public PaymentAttempt get(Long id) {
        return repo.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Payment", String.valueOf(id)));
    }

    @Transactional(readOnly = true)
    public java.util.List<PaymentAttempt> listByBooking(Long bookingId) {
        return repo.findByBookingIdOrderByCreatedAtDesc(bookingId);
    }

    @Transactional(readOnly = true)
    public java.util.List<PaymentAttempt> listByCustomer(Long customerId) {
        return repo.findByCustomerIdOrderByCreatedAtDesc(customerId);
    }
}
