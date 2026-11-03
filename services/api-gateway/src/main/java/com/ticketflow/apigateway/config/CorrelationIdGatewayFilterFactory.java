package com.ticketflow.apigateway.config;

import java.util.UUID;
import org.springframework.cloud.gateway.filter.GatewayFilter;
import org.springframework.cloud.gateway.filter.factory.AbstractGatewayFilterFactory;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.stereotype.Component;

/**
 * Filtre de corrélation — imposé par le cahier des charges (CdC §6.1) pour le suivi
 * des requêtes à travers la chaîne de microservices.
 *
 * Fonctionnement :
 *  - Si la requête entrante contient déjà un header "X-Correlation-Id", on le conserve.
 *  - Sinon, on en génère un (UUID v4) pour cette requête.
 *  - L'identifiant est propagé au service downstream via le header sortant.
 *  - Il est aussi renvoyé au client dans la réponse HTTP pour faciliter le support.
 *
 * Utilisation dans application.yml :
 *
 *   spring:
 *     cloud:
 *       gateway:
 *         server:
 *           webflux:
 *             default-filters:
 *               - CorrelationId
 *
 * Spring Cloud Gateway reconnaît automatiquement le bean `CorrelationIdGatewayFilterFactory`
 * (convention : nom de classe sans "GatewayFilterFactory" → nom court "CorrelationId").
 */
@Component
public class CorrelationIdGatewayFilterFactory
        extends AbstractGatewayFilterFactory<Object> {

    /** Header standard propagé à tous les services internes. */
    public static final String HEADER_NAME = "X-Correlation-Id";

    public CorrelationIdGatewayFilterFactory() {
        super(Object.class);
    }

    @Override
    public GatewayFilter apply(Object config) {
        return (exchange, chain) -> {
            String incoming = exchange.getRequest().getHeaders().getFirst(HEADER_NAME);
            final String id = (incoming == null || incoming.isBlank())
                    ? UUID.randomUUID().toString() : incoming;

            ServerHttpRequest request = exchange.getRequest().mutate()
                    .header(HEADER_NAME, id)
                    .build();

            // Les en-têtes de réponse sont modifiables tant que la réponse n'est pas envoyée.
            // ServerHttpResponse n'expose pas de mutate() : on utilise getHeaders().set(...) directement.
            exchange.getResponse().getHeaders().set(HEADER_NAME, id);

            return chain.filter(exchange.mutate().request(request).build());
        };
    }
}
