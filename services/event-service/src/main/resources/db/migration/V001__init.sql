-- ============================================================================
-- Event Service — Schéma initial
-- Base : ticketflow_event
-- CdC §7.1 : Event, Venue, Category, TicketTier, calendrier, prix public
-- ============================================================================

-- Table : venues
-- Lieu physique. Séparé de Event car réutilisable et filtré par ville (FR-002).
CREATE TABLE venues (
                        id          BIGSERIAL PRIMARY KEY,
                        name        VARCHAR(150) NOT NULL,
                        address     VARCHAR(300) NOT NULL,
                        city        VARCHAR(100) NOT NULL,
                        capacity    INTEGER      NOT NULL,
                        latitude    NUMERIC(9, 6),
                        longitude   NUMERIC(9, 6),
                        created_at  TIMESTAMP WITH TIME ZONE NOT NULL,
                        CONSTRAINT chk_venues_capacity CHECK (capacity > 0)
);

COMMENT ON TABLE  venues IS 'Lieux physiques des événements (CdC §7.1)';
CREATE INDEX idx_venues_city ON venues (city);

-- ----------------------------------------------------------------------------
-- Table : categories
-- Catégorie d'événement (concert, spectacle, sport...). Slug unique.
-- ----------------------------------------------------------------------------
CREATE TABLE categories (
                            id           BIGSERIAL PRIMARY KEY,
                            name         VARCHAR(80)  NOT NULL,
                            slug         VARCHAR(100) NOT NULL UNIQUE,
                            description  VARCHAR(500),
                            created_at   TIMESTAMP WITH TIME ZONE NOT NULL
);

COMMENT ON TABLE categories IS 'Catégories d événements (filtre catalogue FR-002)';

-- ----------------------------------------------------------------------------
-- Table : events
-- Événement billetterie. Le statut contrôle les transitions (FR-004).
-- organizer_id référence customer.id côté customer-service (pas de FK cross-bases).
-- Contrainte : ends_at (si non null) > starts_at — validée aussi côté Java.
-- ----------------------------------------------------------------------------
CREATE TABLE events (
                        id              BIGSERIAL PRIMARY KEY,
                        title           VARCHAR(150) NOT NULL,
                        description     VARCHAR(2000),
                        venue_id        BIGINT       NOT NULL,
                        category_id     BIGINT       NOT NULL,
                        organizer_id    BIGINT       NOT NULL,
                        starts_at       TIMESTAMP WITH TIME ZONE NOT NULL,
                        ends_at         TIMESTAMP WITH TIME ZONE,
                        base_price      NUMERIC(10, 2) NOT NULL,
                        currency        VARCHAR(3)   NOT NULL DEFAULT 'MAD',
                        capacity        INTEGER      NOT NULL,
                        status          VARCHAR(20)  NOT NULL DEFAULT 'DRAFT',
                        published_at    TIMESTAMP WITH TIME ZONE,
                        created_at      TIMESTAMP WITH TIME ZONE NOT NULL,
                        updated_at      TIMESTAMP WITH TIME ZONE NOT NULL,
                        CONSTRAINT chk_events_status CHECK (status IN ('DRAFT', 'PUBLISHED', 'CLOSED', 'ARCHIVED', 'CANCELLED')),
                        CONSTRAINT chk_events_capacity CHECK (capacity > 0),
                        CONSTRAINT chk_events_base_price CHECK (base_price >= 0),
    -- Contrainte métier : ends_at > starts_at (si ends_at est renseigné)
                        CONSTRAINT chk_events_dates CHECK (ends_at IS NULL OR ends_at > starts_at),
                        CONSTRAINT fk_events_venue FOREIGN KEY (venue_id) REFERENCES venues (id),
                        CONSTRAINT fk_events_category FOREIGN KEY (category_id) REFERENCES categories (id)
);

COMMENT ON TABLE  events IS 'Événements billetterie (CdC §7.1, FR-001 à FR-004)';
COMMENT ON COLUMN events.organizer_id IS 'Référence customer.id côté customer-service (pas de FK cross-bases, CdC §7.1)';
COMMENT ON COLUMN events.status IS 'DRAFT -> PUBLISHED -> CLOSED -> ARCHIVED. PUBLISHED -> CANCELLED.';

CREATE INDEX idx_events_status     ON events (status);
CREATE INDEX idx_events_starts_at   ON events (starts_at);
CREATE INDEX idx_events_venue       ON events (venue_id);
CREATE INDEX idx_events_category    ON events (category_id);
CREATE INDEX idx_events_organizer   ON events (organizer_id);

-- ----------------------------------------------------------------------------
-- Table : ticket_tiers
-- Paliers de tarif par événement (CdC §7.1 : TicketTier).
--
-- IMPORTANT : PAS de quantity_sold ici (écriture croisée interdite, CdC §7.1).
-- Le compte des places vendues est détenu par booking-service dans sa propre base.
-- event-service ne sait jamais combien de places ont été vendues.
--
-- version : optimistic locking pour protéger les modifications de capacité.
-- ----------------------------------------------------------------------------
CREATE TABLE ticket_tiers (
                              id                  BIGSERIAL PRIMARY KEY,
                              event_id            BIGINT       NOT NULL,
                              name                VARCHAR(80)  NOT NULL,
                              price               NUMERIC(10, 2) NOT NULL,
                              quantity_available  INTEGER      NOT NULL,
                              version             BIGINT       NOT NULL DEFAULT 0,
                              CONSTRAINT chk_tiers_price CHECK (price >= 0),
                              CONSTRAINT chk_tiers_qty_available CHECK (quantity_available >= 0),
                              CONSTRAINT fk_tiers_event FOREIGN KEY (event_id) REFERENCES events (id) ON DELETE CASCADE
);

COMMENT ON TABLE  ticket_tiers IS 'Paliers de tarif (CdC §7.1). Pas de quantity_sold : booking-service détient le compte.';
COMMENT ON COLUMN ticket_tiers.version IS 'Optimistic locking pour modifications de capacité';

CREATE INDEX idx_tiers_event      ON ticket_tiers (event_id);
CREATE UNIQUE INDEX uk_tiers_event_name ON ticket_tiers (event_id, name);