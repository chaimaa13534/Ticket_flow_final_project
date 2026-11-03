-- ============================================================================
-- Payment Service — Schéma initial
-- Base : ticketflow_payment
-- CdC §7.1 : PaymentAttempt, statut, référence externe, remboursement
-- ============================================================================

-- Table : payment_attempts
-- Une tentative par demande client. idempotency_key unique PAR CLIENT (index partiel).
-- Aucune carte bancaire stockée — uniquement référence externe + statut.
CREATE TABLE payment_attempts (
                                  id                  BIGSERIAL PRIMARY KEY,
                                  booking_id          BIGINT       NOT NULL,
                                  customer_id         BIGINT       NOT NULL,
                                  amount              NUMERIC(12, 2) NOT NULL,
                                  currency            VARCHAR(3)   NOT NULL DEFAULT 'MAD',
                                  status              VARCHAR(25)  NOT NULL DEFAULT 'PENDING',
                                  mode                VARCHAR(15)  NOT NULL,
                                  provider_reference  VARCHAR(100),
                                  idempotency_key     VARCHAR(100),
                                  created_at          TIMESTAMP WITH TIME ZONE NOT NULL,
                                  processed_at        TIMESTAMP WITH TIME ZONE,
                                  failure_reason      VARCHAR(500),
                                  version             BIGINT       NOT NULL DEFAULT 0,
                                  CONSTRAINT chk_pay_status CHECK (status IN ('PENDING', 'SUCCEEDED', 'FAILED', 'CANCELLED', 'REFUNDED', 'PARTIALLY_REFUNDED')),
                                  CONSTRAINT chk_pay_mode CHECK (mode IN ('SIMULATED', 'PROVIDER')),
                                  CONSTRAINT chk_pay_amount CHECK (amount >= 0)
);

COMMENT ON TABLE  payment_attempts IS 'Tentatives de paiement (CdC §7.1, FR-009)';
COMMENT ON COLUMN payment_attempts.booking_id IS 'Référence booking.id côté booking-service (pas de FK cross-bases)';
COMMENT ON COLUMN payment_attempts.customer_id IS 'Référence customer.id côté customer-service (pas de FK cross-bases)';
COMMENT ON COLUMN payment_attempts.provider_reference IS 'Référence externe du prestataire — pas de carte bancaire';
COMMENT ON COLUMN payment_attempts.idempotency_key IS 'Clé d idempotency client — empêche les doubles paiements';
COMMENT ON COLUMN payment_attempts.version IS 'Optimistic locking (transition de statut)';

CREATE INDEX idx_pay_booking       ON payment_attempts (booking_id);
CREATE INDEX idx_pay_customer       ON payment_attempts (customer_id);
CREATE INDEX idx_pay_status         ON payment_attempts (status);
CREATE INDEX idx_pay_provider_ref   ON payment_attempts (provider_reference);

-- Index unique PARTIEL : idempotency_key unique par client, uniquement si non-null.
-- Une ligne avec idempotency_key = NULL n'est pas soumise à l'unicité.
CREATE UNIQUE INDEX uk_pay_idem_customer
    ON payment_attempts (customer_id, idempotency_key)
    WHERE idempotency_key IS NOT NULL;

-- ----------------------------------------------------------------------------
-- Table : refunds
-- Remboursements (CdC FR-012). Plusieurs partiels possibles par paiement.
-- ----------------------------------------------------------------------------
CREATE TABLE refunds (
                         id                   BIGSERIAL PRIMARY KEY,
                         payment_attempt_id   BIGINT       NOT NULL,
                         amount               NUMERIC(12, 2) NOT NULL,
                         type                 VARCHAR(10)  NOT NULL,
                         status               VARCHAR(15)  NOT NULL DEFAULT 'PENDING',
                         provider_reference   VARCHAR(100),
                         reason               VARCHAR(500),
                         created_at           TIMESTAMP WITH TIME ZONE NOT NULL,
                         processed_at         TIMESTAMP WITH TIME ZONE,
                         CONSTRAINT chk_ref_type CHECK (type IN ('PARTIAL', 'FULL')),
                         CONSTRAINT chk_ref_status CHECK (status IN ('PENDING', 'SUCCEEDED', 'FAILED')),
                         CONSTRAINT chk_ref_amount CHECK (amount >= 0),
                         CONSTRAINT fk_refunds_payment FOREIGN KEY (payment_attempt_id) REFERENCES payment_attempts (id)
);

COMMENT ON TABLE refunds IS 'Remboursements (CdC FR-012 : annulation et remboursement)';

CREATE INDEX idx_refunds_payment     ON refunds (payment_attempt_id);
CREATE INDEX idx_refunds_status      ON refunds (status);
CREATE INDEX idx_refunds_provider    ON refunds (provider_reference);