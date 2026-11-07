package com.ticketflow.customerservice.exceptions;

/**
 * Levée quand on tente de créer un client avec un email déjà utilisé — déclenche 409 Conflict.
 *
 * CdC §7.2 : "4xx pour entrée/droit/état invalide".
 */
public class DuplicateEmailException extends RuntimeException {

    private final String email;

    public DuplicateEmailException(String email) {
        super("Email already in use: " + email);
        this.email = email;
    }

    public String getEmail() { return email; }
}
