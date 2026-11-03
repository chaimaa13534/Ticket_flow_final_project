package com.ticketflow.mcptoolserver;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;
import org.springframework.cloud.openfeign.EnableFeignClients;

/**
 * MCP Tool Server — catalogue versionné d'outils allowlistés pour l'assistant agentique.
 *
 * Responsabilités (CdC §6.1, §7.4) :
 *  - Exposer un catalogue d'outils versionnés (search_public_events, get_public_event,
 *    get_my_bookings, get_booking_status).
 *  - Valider strictement les entrées (schéma JSON pour chaque outil).
 *  - Authentifier le service appelant (token statique partagé avec ai-assistant-service).
 *  - Appeler les API métier internes via OpenFeign (jamais le LLM directement).
 *
 * Stateless : aucune persistance. Toute la logique est dans le code, sans dépendance BDD.
 *
 * Le LLM ne reçoit JAMAIS d'accès réseau direct aux services, bases, Kafka ou Eureka.
 * Il demande l'appel d'un outil -> l'AI Assistant Service appelle MCP -> MCP appelle l'API métier.
 */
@SpringBootApplication
@EnableDiscoveryClient
@EnableFeignClients
public class McpToolServerApplication {

    public static void main(String[] args) {
        SpringApplication.run(McpToolServerApplication.class, args);
    }
}
