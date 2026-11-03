package com.ticketflow.aiassistantservice;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;
import org.springframework.cloud.openfeign.EnableFeignClients;

/**
 * AI Assistant Service — orchestrateur agentique + adaptateurs externes.
 *
 * Responsabilités (CdC §6.1, §7.4, FR-015 à FR-019) :
 *  - Adaptateur Telegram : reçoit les webhooks via le Gateway, valide le token, déduplique les updates.
 *  - Orchestration agent : comprend l'intention, appelle les outils MCP autorisés, synthétise.
 *  - Adaptateur LLM interchangeable : un seul client (OpenAI, Claude, Z.ai) via interface.
 *  - Contexte conversationnel à durée limitée (TTL), réinitialisable par l'utilisateur (FR-024).
 *  - Limites d'action (FR-018) : l'agent ne confirme pas de réservation/paiement/annulation seul.
 *
 * Liaison Telegram (FR-017) : initiée depuis une session TicketFlow authentifiée,
 * code unique à usage unique et durée courte. Telegram seul ne prouve jamais l'identité métier.
 *
 * Aucun accès réseau direct du LLM aux services métier : tout passe par le MCP Tool Server.
 */
@SpringBootApplication
@EnableDiscoveryClient
@EnableFeignClients
public class AiAssistantServiceApplication {

    public static void main(String[] args) {
        SpringApplication.run(AiAssistantServiceApplication.class, args);
    }
}
