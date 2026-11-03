package com.ticketflow.bookingservice.exceptions;

/**
 * Levée quand une règle métier de réservation est violée
 * (par ex. réserver un événement non PUBLISHED, ou annuler une réservation déjà CANCELLED).
 *
 * CdC FR-007 et FR-008 : vérification de disponibilité et règles de vente.
 */
public class BookingRuleException extends RuntimeException {

    public BookingRuleException(String message) {
        super(message);
    }
}
