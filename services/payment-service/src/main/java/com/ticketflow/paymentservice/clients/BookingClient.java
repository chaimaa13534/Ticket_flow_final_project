package com.ticketflow.paymentservice.clients;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;

/**
 * Client REST vers booking-service (OpenFeign).
 * Utilisé pour confirmer une réservation après un paiement réussi (FR-010).
 */
@FeignClient(name = "booking-service")
public interface BookingClient {

    @PostMapping("/api/v1/bookings/{id}/confirm")
    void confirmBooking(@PathVariable("id") Long bookingId);
}
