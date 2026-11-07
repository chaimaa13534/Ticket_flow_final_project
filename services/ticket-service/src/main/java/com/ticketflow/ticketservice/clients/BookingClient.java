package com.ticketflow.ticketservice.clients;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

/**
 * Client REST vers booking-service (OpenFeign).
 * Utilisé pour vérifier qu'une réservation est CONFIRMED avant d'émettre un billet (FR-011).
 */
@FeignClient(name = "booking-service")
public interface BookingClient {

    @GetMapping("/api/v1/bookings/{id}")
    BookingView getBooking(@PathVariable("id") Long bookingId);

    /** DTO de lecture d'une réservation (uniquement les champs nécessaires). */
    record BookingView(
            Long id,
            String bookingReference,
            Long eventId,
            String eventTitle,
            Long customerId,
            String status
    ) {}
}
