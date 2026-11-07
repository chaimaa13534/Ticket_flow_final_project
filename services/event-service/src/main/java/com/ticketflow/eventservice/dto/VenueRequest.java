package com.ticketflow.eventservice.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;

public record VenueRequest(
        @NotBlank @Size(max = 150) String name,
        @NotBlank @Size(max = 300) String address,
        @NotBlank @Size(max = 100) String city,
        @NotNull @Positive int capacity,
        BigDecimal latitude,
        BigDecimal longitude
) {}
