-- ============================================================================
-- Notification Service — Schéma initial
-- Base : ticketflow_notification
-- CdC §7.1 : NotificationRequest, canal, tentatives, résultat
-- ============================================================================

-- Table : notification_requests
-- Une demande de notification par événement métier (Kafka) ou par appel direct.
-- UNICITÉ (message_id, channel) — un même événement peut déclencher plusieurs notifications
-- sur des canaux différents (email + Telegram), mais une seule par canal.
CREATE TABLE notification_requests (
                                       id              BIGSERIAL PRIMARY KEY,
                                       message_id      VARCHAR(100) NOT NULL,
                                       customer_id     BIGINT,
                                       recipient       VARCHAR(254) NOT NULL,
                                       channel         VARCHAR(15)  NOT NULL,
                                       template        VARCHAR(80) NOT NULL,
                                       payload         VARCHAR(4000) NOT NULL,
                                       locale          VARCHAR(10)  DEFAULT 'fr',
                                       status          VARCHAR(15)  NOT NULL DEFAULT 'PENDING',
                                       created_at      TIMESTAMP WITH TIME ZONE NOT NULL,
                                       scheduled_at    TIMESTAMP WITH TIME ZONE,
                                       sent_at         TIMESTAMP WITH TIME ZONE,
                                       delivered_at    TIMESTAMP WITH TIME ZONE,
                                       CONSTRAINT chk_notif_channel CHECK (channel IN ('EMAIL', 'TELEGRAM', 'WEBHOOK')),
                                       CONSTRAINT chk_notif_status CHECK (status IN ('PENDING', 'SENT', 'DELIVERED', 'FAILED', 'SUPPRESSED'))
);

COMMENT ON TABLE  notification_requests IS 'Demandes de notification (CdC §7.1, FR-014)';
COMMENT ON COLUMN notification_requests.message_id IS 'Identifiant unique du message Kafka source';
COMMENT ON COLUMN notification_requests.customer_id IS 'Référence customer.id côté customer-service (pas de FK cross-bases)';

CREATE UNIQUE INDEX uk_notif_msg_channel ON notification_requests (message_id, channel);
CREATE INDEX idx_notif_customer           ON notification_requests (customer_id);
CREATE INDEX idx_notif_status             ON notification_requests (status);
CREATE INDEX idx_notif_channel            ON notification_requests (channel);
CREATE INDEX idx_notif_scheduled_at        ON notification_requests (scheduled_at);

-- ----------------------------------------------------------------------------
-- Table : notification_attempts
-- Tentatives d'envoi : une notification peut avoir plusieurs tentatives
-- (reprises avec backoff exponentiel, CdC §6.1).
-- ----------------------------------------------------------------------------
CREATE TABLE notification_attempts (
                                       id                       BIGSERIAL PRIMARY KEY,
                                       notification_request_id  BIGINT      NOT NULL,
                                       attempt_number           INTEGER     NOT NULL,
                                       status                   VARCHAR(15) NOT NULL,
                                       error_message            VARCHAR(1000),
                                       attempted_at             TIMESTAMP WITH TIME ZONE NOT NULL,
                                       duration_ms              BIGINT,
                                       CONSTRAINT chk_notifatt_status CHECK (status IN ('PENDING', 'SENT', 'DELIVERED', 'FAILED', 'SUPPRESSED')),
                                       CONSTRAINT fk_notifatt_request FOREIGN KEY (notification_request_id)
                                           REFERENCES notification_requests (id) ON DELETE CASCADE
);

COMMENT ON TABLE notification_attempts IS 'Tentatives d envoi (CdC §6.1 : reprises observables)';

CREATE INDEX idx_notifatt_request ON notification_attempts (notification_request_id);
CREATE INDEX idx_notifatt_status   ON notification_attempts (status);