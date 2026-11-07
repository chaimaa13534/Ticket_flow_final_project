package com.ticketflow.paymentservice.controllers;

import com.ticketflow.paymentservice.dto.PaymentRequest;
import com.ticketflow.paymentservice.dto.PaymentResponse;
import com.ticketflow.paymentservice.entities.PaymentAttempt;
import com.ticketflow.paymentservice.services.PaymentService;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * API REST Payment — préfixe /api/v1 (CdC §7.2).
 *
 * Endpoints :
 *  - POST /api/v1/payments/simulate : simuler un paiement (FR-009)
 *  - GET /api/v1/payments/{id} : consulter un paiement
 *  - GET /api/v1/payments/by-booking/{bookingId} : lister les paiements d'une réservation
 */
@RestController
@RequestMapping("/api/v1/payments")
public class PaymentController {

    private final PaymentService service;

    public PaymentController(PaymentService service) {
        this.service = service;
    }

    @PostMapping("/simulate")
    public ResponseEntity<PaymentResponse> simulate(@Valid @RequestBody PaymentRequest req) {
        PaymentAttempt payment = service.simulatePayment(req);
        return ResponseEntity.ok(PaymentResponse.from(payment));
    }

    @GetMapping("/{id}")
    public PaymentResponse get(@PathVariable Long id) {
        return PaymentResponse.from(service.get(id));
    }

    @GetMapping("/by-booking/{bookingId}")
    public List<PaymentResponse> listByBooking(@PathVariable Long bookingId) {
        return service.listByBooking(bookingId).stream().map(PaymentResponse::from).toList();
    }
}
