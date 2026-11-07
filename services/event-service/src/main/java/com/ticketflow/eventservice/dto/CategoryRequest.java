package com.ticketflow.eventservice.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record CategoryRequest(
        @NotBlank @Size(max = 80) String name,
        @NotBlank @Pattern(regexp = "^[a-z0-9-]+$", message = "must be lowercase with hyphens")
        @Size(max = 100) String slug,
        @Size(max = 500) String description
) {}
