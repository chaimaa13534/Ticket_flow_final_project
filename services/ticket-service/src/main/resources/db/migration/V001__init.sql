-- ============================================================================
-- Ticket Service — Schéma initial
-- Base : ticketflow_ticket
-- CdC §7.1 : Ticket, code vérifiable, statut d'usage
-- ============================================================================

-- Table : tickets
-- Billet unique émis après confirmation de réservation (FR-011).
-- Le code est NON prédictible (UUID + HMAC) — ne contient aucune donnée personnelle (CdC §3.5).
--
-- UNICITÉ (anti-rejeu Kafka) :
--  - UNIQUE (code) : chaque billet a un code distinct (immuable).
--  - UNIQUE (booking_id, ticket_tier_id) : un événement Kafka rejoué ne peut pas créer
--    de billet en double pour le même palier de la même réservation.
CREATE TABLE tickets (
                         id                BIGSERIAL PRIMARY KEY,
                         code              VARCHAR(64)  NOT NULL UNIQUE,
                         booking_id        BIGINT       NOT NULL,
                         ticket_tier_id    BIGINT       NOT NULL,
                         customer_id       BIGINT       NOT NULL,
                         event_id          BIGINT       NOT NULL,
                         event_title       VARCHAR(150) NOT NULL,
                         event_starts_at   TIMESTAMP WITH TIME ZONE NOT NULL,
                         tier_name         VARCHAR(80)  NOT NULL,
                         status            VARCHAR(15)  NOT NULL DEFAULT 'ISSUED',
                         issued_at         TIMESTAMP WITH TIME ZONE NOT NULL,
                         used_at           TIMESTAMP WITH TIME ZONE,
                         expired_at        TIMESTAMP WITH TIME ZONE,
                         cancelled_at      TIMESTAMP WITH TIME ZONE,
                         version           BIGINT       NOT NULL DEFAULT 0,
                         CONSTRAINT chk_tickets_status CHECK (status IN ('ISSUED', 'USED', 'EXPIRED', 'CANCELLED'))
);

COMMENT ON TABLE  tickets IS 'Billets émis après confirmation (CdC §7.1, FR-011)';
COMMENT ON COLUMN tickets.code IS 'Code de vérification NON prédictible (UUID + HMAC) — ne contient aucune donnée personnelle';
COMMENT ON COLUMN tickets.booking_id IS 'Référence booking.id côté booking-service (pas de FK cross-bases)';
COMMENT ON COLUMN tickets.customer_id IS 'Référence customer.id côté customer-service (pas de FK cross-bases)';
COMMENT ON COLUMN tickets.event_id IS 'Snapshot event.id — permet la recherche sans appel cross-service';
COMMENT ON COLUMN tickets.version IS 'Optimistic locking (transition ISSUED -> USED)';

CREATE UNIQUE INDEX uk_tickets_code        ON tickets (code);
CREATE UNIQUE INDEX uk_tickets_booking_tier ON tickets (booking_id, ticket_tier_id);
CREATE INDEX idx_tickets_customer          ON tickets (customer_id);
CREATE INDEX idx_tickets_event              ON tickets (event_id);
CREATE INDEX idx_tickets_status             ON tickets (status);