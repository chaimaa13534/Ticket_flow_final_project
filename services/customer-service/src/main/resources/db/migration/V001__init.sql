-- ============================================================================
-- Customer Service — Schéma initial
-- Base : ticketflow_customer
-- CdC §7.1 : Customer, profil, préférences, liaison de canal
-- ============================================================================

-- Table : customers
-- Compte client TicketFlow. Le keycloak_id fait le lien avec l'identité Keycloak
-- (sub du JWT) sans duplication des credentials.
CREATE TABLE customers (
                           id                BIGSERIAL PRIMARY KEY,
                           keycloak_id       VARCHAR(100)  NOT NULL UNIQUE,
                           first_name        VARCHAR(100)  NOT NULL,
                           last_name         VARCHAR(100)  NOT NULL,
                           email             VARCHAR(254)  NOT NULL UNIQUE,
                           phone             VARCHAR(30),
                           preferred_locale  VARCHAR(10)   DEFAULT 'fr',
                           status            VARCHAR(20)   NOT NULL DEFAULT 'ACTIVE',
                           created_at        TIMESTAMP WITH TIME ZONE NOT NULL,
                           updated_at        TIMESTAMP WITH TIME ZONE NOT NULL,
                           version           BIGINT        NOT NULL DEFAULT 0,
                           CONSTRAINT chk_customer_status CHECK (status IN ('ACTIVE', 'SUSPENDED', 'CLOSED'))
);

COMMENT ON TABLE  customers IS 'Comptes clients TicketFlow (propriétaire : customer-service)';
COMMENT ON COLUMN customers.keycloak_id IS 'sub du JWT Keycloak — unique, immuable après création';
COMMENT ON COLUMN customers.status IS 'ACTIVE | SUSPENDED | CLOSED (CdC FR-005)';
COMMENT ON COLUMN customers.version IS 'Optimistic locking (transition statut)';

-- Index uniques sur email et keycloak_id déjà déclarés via CONSTRAINT UNIQUE.

-- ----------------------------------------------------------------------------
-- Table : customer_profiles
-- Préférences notifications du client (1-1 strict avec customers).
-- PK partagée avec customers.id (@MapsId côté JPA) — pas de BIGSERIAL ici.
-- ----------------------------------------------------------------------------
CREATE TABLE customer_profiles (
                                   id                 BIGINT PRIMARY KEY REFERENCES customers (id) ON DELETE CASCADE,
                                   email_enabled      BOOLEAN NOT NULL DEFAULT TRUE,
                                   telegram_enabled   BOOLEAN NOT NULL DEFAULT FALSE,
                                   marketing_opt_in   BOOLEAN NOT NULL DEFAULT FALSE,
                                   updated_at         TIMESTAMP WITH TIME ZONE NOT NULL
);

COMMENT ON TABLE customer_profiles IS 'Préférences notification (CdC FR-014). PK partagée avec customers.';

-- ----------------------------------------------------------------------------
-- Table : telegram_links
-- Liaison compte TicketFlow <-> Telegram (CdC FR-017).
--
-- RÉ-LIAISON (FR-017) :
--  - Index unique PARTIEL sur customer_id WHERE status IN ('PENDING','VERIFIED')
--    => autorise une nouvelle liaison après REVOKED ou EXPIRED.
--  - Index unique PARTIEL sur telegram_user_id WHERE status = 'VERIFIED'
--    => un user Telegram ne peut être lié qu'à un seul client vérifié à la fois.
-- ----------------------------------------------------------------------------
CREATE TABLE telegram_links (
                                id                BIGSERIAL PRIMARY KEY,
                                customer_id       BIGINT       NOT NULL,
                                telegram_user_id  VARCHAR(64),
                                telegram_chat_id  VARCHAR(64),
                                link_code         VARCHAR(32)  NOT NULL UNIQUE,
                                status            VARCHAR(20)  NOT NULL DEFAULT 'PENDING',
                                created_at        TIMESTAMP WITH TIME ZONE NOT NULL,
                                expires_at        TIMESTAMP WITH TIME ZONE NOT NULL,
                                verified_at       TIMESTAMP WITH TIME ZONE,
                                revoked_at        TIMESTAMP WITH TIME ZONE,
                                version           BIGINT       NOT NULL DEFAULT 0,
                                CONSTRAINT chk_tglink_status CHECK (status IN ('PENDING', 'VERIFIED', 'REVOKED', 'EXPIRED'))
);

COMMENT ON TABLE  telegram_links IS 'Liaisons Telegram <-> comptes clients (CdC FR-017)';
COMMENT ON COLUMN telegram_links.link_code IS 'Code unique à usage unique, durée courte — généré côté TicketFlow';
COMMENT ON COLUMN telegram_links.version IS 'Optimistic locking (transition PENDING -> VERIFIED)';

-- Index unique PARTIEL : seule une liaison PENDING ou VERIFIED est unique par client.
-- Permet la re-liaison après REVOKED ou EXPIRED.
CREATE UNIQUE INDEX uk_tglink_customer_active
    ON telegram_links (customer_id)
    WHERE status IN ('PENDING', 'VERIFIED');

-- Index unique PARTIEL : un telegram_user_id ne peut être VERIFIED que pour un seul client.
CREATE UNIQUE INDEX uk_tglink_telegram_verified
    ON telegram_links (telegram_user_id)
    WHERE status = 'VERIFIED';

-- Index secondaire pour recherche par code (déjà UNIQUE au niveau colonne, redondant ici)
-- et pour les scans par statut.
CREATE INDEX idx_tglink_status ON telegram_links (status);
