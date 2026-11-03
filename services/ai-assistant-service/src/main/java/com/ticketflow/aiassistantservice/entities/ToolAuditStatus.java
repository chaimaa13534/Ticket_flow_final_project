package com.ticketflow.aiassistantservice.entities;

/**
 * Statut d'exécution d'un outil MCP (CdC §7.4, FR-020 : audit technique des outils).
 *
 * - ALLOWED   : l'outil figure dans l'allowlist — l'appel a été autorisé.
 * - BLOCKED   : l'outil n'est PAS dans l'allowlist — l'appel a été refusé.
 *               (sécurité : le LLM ne peut pas demander un outil non prévu)
 * - EXECUTED  : l'outil a été exécuté avec succès.
 * - FAILED    : l'outil a échoué (erreur API métier, timeout, etc.).
 *
 * CdC §7.4 et FR-018 : certaines actions sont INTERDITES (confirm_booking,
 * process_payment, cancel_booking, refund) — l'appel serait BLOCKED.
 */
public enum ToolAuditStatus {
    ALLOWED,
    BLOCKED,
    EXECUTED,
    FAILED
}
