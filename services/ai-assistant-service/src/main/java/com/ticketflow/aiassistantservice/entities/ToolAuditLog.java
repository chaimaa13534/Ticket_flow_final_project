package com.ticketflow.aiassistantservice.entities;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import java.time.Instant;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * Audit technique des outils MCP appelés (CdC §7.4, FR-020).
 *
 * On ne persiste PAS les entrées/sorties brutes (sécurité + volume), mais :
 *  - Le nom et la version de l'outil appelé (auditabilité).
 *  - Le statut (ALLOWED / BLOCKED / EXECUTED / FAILED).
 *  - Un hash de l'entrée (pour repérer les patterns sans stocker le contenu).
 *  - {@code conversationToken} : identifiant opaque de la conversation (UUID, pas la PK).
 *  - {@code correlationId} : permet de relier cet audit à la trace distribuée (CdC FR-020).
 *  - {@code actor} : identité de l'utilisateur (telegram_user_id ou customer_id si lié).
 *
 * RÈGLE : un outil non dans l'allowlist est BLOCKED — le LLM ne peut pas
 * exécuter d'action non prévue (CdC §7.4, FR-018).
 *
 * AUCUNE FK vers conversation_contexts : la colonne conversation_token est un UUID
 * stocké sans contrainte d'intégrité. Quand un contexte expire et est purgé (CdC FR-024),
 * les lignes d'audit RESTENT pour auditabilité (CdC FR-020).
 */
@Entity
@Table(name = "tool_audit_logs",
        indexes = {
                @Index(name = "idx_audit_conversation", columnList = "conversation_token"),
                @Index(name = "idx_audit_tool", columnList = "tool_name"),
                @Index(name = "idx_audit_status", columnList = "status"),
                @Index(name = "idx_audit_executed_at", columnList = "executed_at"),
                @Index(name = "idx_audit_correlation", columnList = "correlation_id")
        })
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class ToolAuditLog {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** Token opaque de la conversation (UUID, pas la PK numérique). */
    @Column(name = "conversation_token", nullable = false, length = 36)
    private String conversationToken;

    /** Nom de l'outil demandé (search_public_events, get_my_bookings, etc.). */
    @Column(name = "tool_name", nullable = false, length = 80)
    private String toolName;

    /** Version de l'outil (CdC §7.4 : "catalogue versionné d'outils"). */
    @Column(name = "tool_version", nullable = false, length = 10)
    private String toolVersion;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 10)
    private ToolAuditStatus status;

    /** Hash SHA-256 de l'entrée — permet l'audit sans stocker le contenu. */
    @Column(name = "input_hash", length = 64)
    private String inputHash;

    /** Message d'erreur si FAILED (jamais de payload sensible). */
    @Column(name = "error_message", length = 500)
    private String errorMessage;

    /** Corrélation distribuée (X-Correlation-Id propagé par le gateway). */
    @Column(name = "correlation_id", length = 64)
    private String correlationId;

    /**
     * Acteur : identité de l'utilisateur à l'origine de l'appel.
     * - Si non lié : telegram_user_id.
     * - Si lié : customer_id (référence customer.id côté customer-service, pas de FK cross-bases).
     * CdC FR-020 : "acteur, action, ressource, horodatage et corrélation".
     */
    @Column(name = "actor", length = 100)
    private String actor;

    @Column(name = "executed_at", nullable = false, updatable = false)
    private Instant executedAt;

    @Column(name = "duration_ms")
    private Long durationMs;

    public ToolAuditLog(String conversationToken, String toolName, String toolVersion,
                        ToolAuditStatus status, String inputHash, String errorMessage,
                        String correlationId, String actor, Long durationMs) {
        this.conversationToken = conversationToken;
        this.toolName = toolName;
        this.toolVersion = toolVersion;
        this.status = status;
        this.inputHash = inputHash;
        this.errorMessage = errorMessage;
        this.correlationId = correlationId;
        this.actor = actor;
        this.durationMs = durationMs;
        this.executedAt = Instant.now();
    }
}
