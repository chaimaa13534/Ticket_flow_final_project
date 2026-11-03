-- ============================================================================
-- Booking Service — Schéma initial
-- Base : ticketflow_booking
-- CdC §7.1 : Booking, BookingLine, réservation temporaire, snapshot prix, idempotency key
-- ============================================================================

-- Table : bookings
-- Entête de réservation. event_id et customer_id sont des références cross-service
-- (pas de FK, CdC §7.1 dernière phrase). booking_reference est l'identifiant stable client.
CREATE TABLE bookings (
                          id                 BIGSERIAL PRIMARY KEY,
                          booking_reference  VARCHAR(30)  NOT NULL UNIQUE,
                          event_id           BIGINT       NOT NULL,
                          event_title        VARCHAR(150) NOT NULL,
                          customer_id         BIGINT       NOT NULL,
    -- NOTE : idempotency_key est stockée dans la table dédiée idempotency_keys (source de vérité).
    -- On ne la duplique PAS ici pour éviter toute incohérence (CdC §7.1).
                          total_amount       NUMERIC(12, 2) NOT NULL,
                          currency           VARCHAR(3)   NOT NULL DEFAULT 'MAD',
                          status             VARCHAR(20)  NOT NULL DEFAULT 'PENDING',
                          created_at         TIMESTAMP WITH TIME ZONE NOT NULL,
                          expires_at         TIMESTAMP WITH TIME ZONE,
                          confirmed_at       TIMESTAMP WITH TIME ZONE,
                          cancelled_at       TIMESTAMP WITH TIME ZONE,
                          expired_at         TIMESTAMP WITH TIME ZONE,
                          version            BIGINT       NOT NULL DEFAULT 0,
                          CONSTRAINT chk_bookings_status CHECK (status IN ('PENDING', 'CONFIRMED', 'CANCELLED', 'EXPIRED', 'REFUNDED')),
                          CONSTRAINT chk_bookings_amount CHECK (total_amount >= 0)
);

COMMENT ON TABLE  bookings IS 'Réservations (CdC §7.1, FR-007 à FR-010)';
COMMENT ON COLUMN bookings.event_id IS 'Référence event.id côté event-service (pas de FK cross-bases)';
COMMENT ON COLUMN bookings.customer_id IS 'Référence customer.id côté customer-service (pas de FK cross-bases)';
COMMENT ON COLUMN bookings.event_title IS 'Snapshot du titre au moment de la réservation (cohérence historique)';
COMMENT ON COLUMN bookings.version IS 'Optimistic locking (transition PENDING -> CONFIRMED)';
-- NOTE : la clé d idempotency est stockée dans la table idempotency_keys (source de vérité).
-- La FK idempotency_keys.booking_id -> bookings.id n est PAS déclarée ici (pas de FK cross-service
-- dans le sens inverse : un booking peut exister sans entrée idempotency_key si créé sans header).

CREATE INDEX idx_bookings_customer            ON bookings (customer_id);
CREATE INDEX idx_bookings_event              ON bookings (event_id);
CREATE INDEX idx_bookings_status             ON bookings (status);
CREATE INDEX idx_bookings_expires_at         ON bookings (expires_at);

-- ----------------------------------------------------------------------------
-- Table : booking_lines
-- Lignes de réservation (un booking = plusieurs lignes, une ligne par TicketTier).
-- unit_price et tier_name sont des snapshots (CdC §7.1).
-- ----------------------------------------------------------------------------
CREATE TABLE booking_lines (
                               id              BIGSERIAL PRIMARY KEY,
                               booking_id      BIGINT       NOT NULL,
                               ticket_tier_id  BIGINT       NOT NULL,
                               tier_name       VARCHAR(80)  NOT NULL,
                               unit_price      NUMERIC(10, 2) NOT NULL,
                               quantity        INTEGER      NOT NULL,
                               line_total      NUMERIC(12, 2) NOT NULL,
                               CONSTRAINT chk_blines_qty CHECK (quantity >= 1),
                               CONSTRAINT chk_blines_unit_price CHECK (unit_price >= 0),
                               CONSTRAINT chk_blines_total CHECK (line_total = unit_price * quantity),
                               CONSTRAINT fk_blines_booking FOREIGN KEY (booking_id) REFERENCES bookings (id) ON DELETE CASCADE
);

COMMENT ON TABLE  booking_lines IS 'Lignes de réservation (CdC §7.1)';
COMMENT ON COLUMN booking_lines.ticket_tier_id IS 'Référence ticket_tier.id côté event-service (pas de FK cross-bases)';
COMMENT ON COLUMN booking_lines.unit_price IS 'Snapshot du prix au moment de la réservation';

CREATE INDEX idx_blines_booking ON booking_lines (booking_id);
CREATE INDEX idx_blines_tier    ON booking_lines (ticket_tier_id);

-- ----------------------------------------------------------------------------
-- Table : tier_inventory
-- Inventaire des places PAR PALIER DE TARIF (FR-007, FR-008 : atomique anti-concurrence).
-- PK = ticket_tier_id (référence ticket_tier.id côté event-service, pas de FK cross-bases).
--
-- IMPORTANT : c'est ICI que le compte des ventes est tenu, dans la base du booking-service.
-- event-service ne sait jamais combien de places ont été vendues (pas d écriture croisée, CdC §7.1).
-- held = places PENDING ; sold = places CONFIRMED.
-- Capacité disponible = capacity - held - sold (jamais négatif).
-- ----------------------------------------------------------------------------
CREATE TABLE tier_inventory (
                                ticket_tier_id  BIGINT PRIMARY KEY,
                                capacity        INTEGER NOT NULL,
                                held            INTEGER NOT NULL DEFAULT 0,
                                sold            INTEGER NOT NULL DEFAULT 0,
                                version         BIGINT  NOT NULL DEFAULT 0,
                                CONSTRAINT chk_tierinv_capacity CHECK (capacity >= 0),
                                CONSTRAINT chk_tierinv_held CHECK (held >= 0),
                                CONSTRAINT chk_tierinv_sold CHECK (sold >= 0)
);

COMMENT ON TABLE  tier_inventory IS 'Inventaire atomique PAR PALIER (FR-007, FR-008). Source de vérité des ventes.';
COMMENT ON COLUMN tier_inventory.version IS 'Optimistic locking Spring Data JPA';

-- ----------------------------------------------------------------------------
-- Table : idempotency_keys
-- Trace des Idempotency-Key consommées par (customerId, key) — empêche les doubles créations.
-- ----------------------------------------------------------------------------
CREATE TABLE idempotency_keys (
                                  id               BIGSERIAL PRIMARY KEY,
                                  customer_id      BIGINT       NOT NULL,
                                  idempotency_key  VARCHAR(100) NOT NULL,
                                  booking_id       BIGINT,
                                  created_at       TIMESTAMP WITH TIME ZONE NOT NULL,
                                  expires_at       TIMESTAMP WITH TIME ZONE NOT NULL
);

COMMENT ON TABLE idempotency_keys IS 'Trace d idempotency (CdC §7.1, FR-008)';

CREATE UNIQUE INDEX uk_idem_key_customer ON idempotency_keys (customer_id, idempotency_key);
CREATE INDEX idx_idem_expires_at          ON idempotency_keys (expires_at);