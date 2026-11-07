package com.ticketflow.customerservice.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * Corps de requête pour créer ou mettre à jour un client.
 *
 * Le {@code keycloakId} n'est PAS dans le DTO : il sera extrait du JWT côté API
 * (étape 4). Le client ne peut JAMAIS fournir son propre keycloakId.
 *
 * Validation : Bean Validation active (jakarta.validation.constraints).
 */
public record CustomerRequest(
        @NotBlank @Size(max = 100) String firstName,
        @NotBlank @Size(max = 100) String lastName,
        @NotBlank @Email @Size(max = 254) String email,
        @Size(max = 30) String phone
) {}