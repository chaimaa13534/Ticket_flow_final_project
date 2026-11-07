package com.ticketflow.bookingservice.exceptions;

/**
 * Levée quand un service upstream (event-service, customer-service) est indisponible
 * ou renvoie une erreur 5xx — déclenche 503 Service Unavailable.
 *
 * CdC §8.1 : "délais, retries et circuit breakers sont explicites" — Resilience4J gère
 * les retries côté Feign, mais si tout échoue, on remonte cette exception.
 */
public class UpstreamUnavailableException extends RuntimeException {

    private final String upstreamService;

    public UpstreamUnavailableException(String upstreamService, Throwable cause) {
        super("Upstream service '" + upstreamService + "' is unavailable: " + cause.getMessage(), cause);
        this.upstreamService = upstreamService;
    }

    public String getUpstreamService() { return upstreamService; }
}
