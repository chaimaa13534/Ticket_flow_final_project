package com.ticketflow.aiassistantservice.entities;

/**
 * État d'une conversation agentique (CdC FR-024 : réinitialisation du contexte).
 *
 * La machine à états est volontairement simple :
 *
 *   INIT  ──user_msg──▶  AWAITING_QUERY
 *   AWAITING_QUERY  ──tool_call──▶  AWAITING_TOOL
 *   AWAITING_TOOL  ──tool_result──▶  AWAITING_LLM
 *   AWAITING_LLM  ──llm_response──▶  COMPLETED
 *
 * Si l'utilisateur demande des données personnelles :
 *   AWAITING_QUERY  ──needs_link──▶  AWAITING_LINKAGE
 *
 * Erreur à n'importe quel étape → ERROR (avec possibilité de reset).
 */
public enum ConversationState {
    INIT,
    AWAITING_QUERY,
    AWAITING_TOOL,
    AWAITING_LLM,
    AWAITING_LINKAGE,
    COMPLETED,
    ERROR
}