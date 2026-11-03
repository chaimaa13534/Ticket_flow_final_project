package com.ticketflow.eventservice.exceptions;

/**
 * Levée quand une transition d'état d'événement est invalide
 * (par ex. publier un événement déjà publié, ou baisser la capacité d'un événement publié).
 *
 * CdC FR-004 : les opérations sur les événements suivent des règles d'état explicites.
 */
public class InvalidEventStateException extends RuntimeException {

    public InvalidEventStateException(String message) {
        super(message);
    }
}
