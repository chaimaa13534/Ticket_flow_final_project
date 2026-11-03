-- ============================================================================
-- AI Assistant Service — Schéma initial
-- Base : ticketflow_assistant
-- CdC §7.1 : Lien Telegram, identifiant opaque de conversation, TTL de session, audit technique
-- ============================================================================

-- Table : telegram_update_logs
-- Déduplication des updates Telegram (CdC FR-019). update_id est unique Telegram-side.
CREATE TABLE telegram_update_logs (
                                      id                  BIGSERIAL PRIMARY KEY,
                                      update_id           BIGINT      NOT NULL UNIQUE,
                                      telegram_user_id    VARCHAR(64),
                                      received_at         TIMESTAMP WITH TIME ZONE NOT NULL,
                                      processed_at        TIMESTAMP WITH TIME ZONE,
                                      status              VARCHAR(15) NOT NULL DEFAULT 'RECEIVED',
                                      payload_size_bytes  INTEGER,
                                      CONSTRAINT chk_tgupd_status CHECK (status IN ('RECEIVED', 'PROCESSED', 'DUPLICATE', 'UNSUPPORTED'))
);

COMMENT ON TABLE  telegram_update_logs IS 'Log de déduplication des updates Telegram (CdC FR-019)';
COMMENT ON COLUMN telegram_update_logs.update_id IS 'Identifiant unique Telegram — garantie de déduplication';

CREATE INDEX idx_tgupd_telegram_user ON telegram_update_logs (telegram_user_id);
CREATE INDEX idx_tgupd_received_at    ON telegram_update_logs (received_at);

-- ----------------------------------------------------------------------------
-- Table : conversation_contexts
-- Contexte conversationnel à durée limitée (CdC FR-024).
-- conversation_token : UUID opaque exposé à l agent et au client. La PK numérique n est jamais partagée.
-- ----------------------------------------------------------------------------
CREATE TABLE conversation_contexts (
                                       id                  BIGSERIAL PRIMARY KEY,
                                       conversation_token  VARCHAR(36)  NOT NULL UNIQUE,
                                       telegram_user_id    VARCHAR(64)  NOT NULL UNIQUE,
                                       customer_link_id    BIGINT,
                                       state               VARCHAR(20)  NOT NULL DEFAULT 'INIT',
                                       history             VARCHAR(8000),
                                       created_at          TIMESTAMP WITH TIME ZONE NOT NULL,
                                       updated_at          TIMESTAMP WITH TIME ZONE NOT NULL,
                                       expires_at          TIMESTAMP WITH TIME ZONE NOT NULL,
                                       CONSTRAINT chk_ctx_state CHECK (state IN ('INIT', 'AWAITING_QUERY', 'AWAITING_TOOL',
                                                                                 'AWAITING_LLM', 'AWAITING_LINKAGE', 'COMPLETED', 'ERROR'))
);

COMMENT ON TABLE  conversation_contexts IS 'Contexte conversationnel à durée limitée (CdC FR-024)';
COMMENT ON COLUMN conversation_contexts.conversation_token IS 'UUID opaque exposé à l agent (jamais la PK numérique)';
COMMENT ON COLUMN conversation_contexts.history IS 'Historique synthétisé (JSON) — jamais le contenu brut';

CREATE INDEX idx_ctx_expires_at ON conversation_contexts (expires_at);

-- ----------------------------------------------------------------------------
-- Table : tool_audit_logs
-- Audit technique des outils MCP appelés (CdC §7.4, FR-020).
--
-- AUCUNE FK vers conversation_contexts : la colonne conversation_token est un UUID
-- stocké pour référence, sans contrainte d'intégrité. Ainsi, quand deleteExpired purge
-- les contextes (TTL, CdC FR-024), les audits RESTENT pour auditabilité (CdC FR-020).
-- On référence le conversation_token (UUID) plutôt que la PK numérique de conversation_contexts.
-- ----------------------------------------------------------------------------
CREATE TABLE tool_audit_logs (
                                 id                          BIGSERIAL PRIMARY KEY,
                                 conversation_token          VARCHAR(36)  NOT NULL,
                                 tool_name                   VARCHAR(80) NOT NULL,
                                 tool_version                VARCHAR(10) NOT NULL,
                                 status                      VARCHAR(10) NOT NULL,
                                 input_hash                  VARCHAR(64),
                                 error_message               VARCHAR(500),
                                 correlation_id              VARCHAR(64),
                                 actor                       VARCHAR(100),
                                 executed_at                 TIMESTAMP WITH TIME ZONE NOT NULL,
                                 duration_ms                 BIGINT,
                                 CONSTRAINT chk_audit_status CHECK (status IN ('ALLOWED', 'BLOCKED', 'EXECUTED', 'FAILED'))
);

COMMENT ON TABLE  tool_audit_logs IS 'Audit technique des outils MCP (CdC §7.4, FR-020)';
COMMENT ON COLUMN tool_audit_logs.conversation_token IS 'UUID opaque de la conversation — AUCUNE FK vers conversation_contexts (audit survit à la purge des contextes, CdC FR-020)';
COMMENT ON COLUMN tool_audit_logs.input_hash IS 'Hash SHA-256 de l entrée — permet audit sans stocker le contenu';
COMMENT ON COLUMN tool_audit_logs.correlation_id IS 'Corrélation distribuée X-Correlation-Id propagée par le gateway (CdC FR-020)';
COMMENT ON COLUMN tool_audit_logs.actor IS 'Identité utilisateur (telegram_user_id ou customer_id si lié) — CdC FR-020';
COMMENT ON COLUMN tool_audit_logs.status IS 'ALLOWED | BLOCKED (allowlist) | EXECUTED | FAILED';

CREATE INDEX idx_audit_conversation ON tool_audit_logs (conversation_token);
CREATE INDEX idx_audit_tool          ON tool_audit_logs (tool_name);
CREATE INDEX idx_audit_status        ON tool_audit_logs (status);
CREATE INDEX idx_audit_executed_at   ON tool_audit_logs (executed_at);
CREATE INDEX idx_audit_correlation   ON tool_audit_logs (correlation_id);

-- ----------------------------------------------------------------------------
-- Table : customer_links
-- Liaison locale Telegram <-> Customer (vue agentique). Synchronisée via Kafka
-- à partir de customer-service.TelegramLink.
--
-- RÉ-LIAISON (FR-017) : index unique PARTIEL sur customer_id WHERE status='VERIFIED'
-- et sur telegram_user_id WHERE status='VERIFIED' — autorise une nouvelle liaison
-- après REVOKED.
-- ----------------------------------------------------------------------------
CREATE TABLE customer_links (
                                id                  BIGSERIAL PRIMARY KEY,
                                customer_id         BIGINT      NOT NULL,
                                telegram_user_id    VARCHAR(64) NOT NULL,
                                status              VARCHAR(20) NOT NULL,
                                linked_at           TIMESTAMP WITH TIME ZONE NOT NULL,
                                revoked_at          TIMESTAMP WITH TIME ZONE,
                                CONSTRAINT chk_clink_status CHECK (status IN ('VERIFIED', 'REVOKED'))
);

COMMENT ON TABLE customer_links IS 'Liaisons Telegram <-> Customer (vue agentique, synchronisée via Kafka)';

-- Index uniques PARTIELS : un client / un telegram_user_id ne peut avoir qu une seule
-- liaison VERIFIED à la fois. Autorise la re-liaison après REVOKED.
CREATE UNIQUE INDEX uk_clink_customer_verified
    ON customer_links (customer_id)
    WHERE status = 'VERIFIED';

CREATE UNIQUE INDEX uk_clink_telegram_verified
    ON customer_links (telegram_user_id)
    WHERE status = 'VERIFIED';