package com.ticketflow.customerservice.dto;

import com.ticketflow.customerservice.entities.Customer;
import com.ticketflow.customerservice.entities.CustomerStatus;
import java.time.Instant;

/**
 * Réponse standard exposée au client. Ne contient PAS le keycloakId (interne),
 * ni le version (détail d'implémentation).
 *
 * La conversion Entity -> Response est volontairement explicite (pas de MapStruct)
 * pour rester pédagogique.
 */
public record CustomerResponse(
        Long id,
        String firstName,
        String lastName,
        String email,
        String phone,
        String preferredLocale,
        CustomerStatus status,
        Instant createdAt,
        Instant updatedAt
) {
    public static CustomerResponse from(Customer c) {
        return new CustomerResponse(
                c.getId(),
                c.getFirstName(),
                c.getLastName(),
                c.getEmail(),
                c.getPhone(),
                c.getPreferredLocale(),
                c.getStatus(),
                c.getCreatedAt(),
                c.getUpdatedAt()
        );
    }
}
