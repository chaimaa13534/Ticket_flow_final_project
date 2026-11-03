package com.ticketflow.paymentservice.entities;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import jakarta.persistence.Version;
import java.math.BigDecimal;
import java.time.Instant;

/**
 * Tentative de paiement (CdC §7.1, FR-009).
 *
 * RÔLE :
 *  - Une tentative par demande client (pas de double paiement via idempotencyKey).
 *  - {@code bookingId} référence le booking côté booking-service (pas de FK cross-bases).
 *  - {@code customerId} est l'identité issue du JWT (jamais du body de requête).
 *  - {@code providerReference} est la référence externe du prestataire (Stripe charge id, etc.).
 *
 * Le mode {@code SIMULATED} permet de piloter succès/échec/annulation pour la démo.
 * Le mode {@code PROVIDER} active l'adaptateur externe.
 *
 * {@code @Version} active l'optimistic locking : deux callbacks de paiement simultanés
 * ne peuvent pas confirmer deux fois la même tentative (CdC FR-010).
 */
@Entity
@Table(name = "payment_attempts",
        indexes = {
                @Index(name = "idx_pay_booking", columnList = "booking_id"),
                @Index(name = "idx_pay_customer", columnList = "customer_id"),
                @Index(name = "idx_pay_status", columnList = "status"),
                // Index unique PARTIEL : idempotency_key unique PAR CLIENT et uniquement si non-null.
                // Une ligne avec idempotency_key = NULL n'est pas soumise à l'unicité (utile pour
                // les paiements internes sans clé explicite).
                @Index(name = "idx_pay_provider_ref", columnList = "provider_reference")
        })
public class PaymentAttempt {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "booking_id", nullable = false)
    private Long bookingId;

    @Column(name = "customer_id", nullable = false)
    private Long customerId;

    @Column(nullable = false, precision = 12, scale = 2)
    private BigDecimal amount;

    @Column(nullable = false, length = 3)
    private String currency = "MAD";

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 25)
    private PaymentStatus status = PaymentStatus.PENDING;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 15)
    private PaymentMode mode;

    /** Référence externe du prestataire (charge id Stripe, etc.). Null en mode SIMULATED. */
    @Column(name = "provider_reference", length = 100)
    private String providerReference;

    /**
     * Clé d'idempotency fournie par le client (FR-009 : callbacks idempotents).
     * Unicité partielle (customer_id, idempotency_key) WHERE idempotency_key IS NOT NULL
     * — imposée côté BDD dans la migration Flyway.
     */
    @Column(name = "idempotency_key", length = 100)
    private String idempotencyKey;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "processed_at")
    private Instant processedAt;

    /** Message en cas d'échec (jamais de données bancaires). */
    @Column(name = "failure_reason", length = 500)
    private String failureReason;

    /** Optimistic locking — protège les transitions de statut contre la concurrence (FR-010). */
    @Version
    @Column(name = "version", nullable = false)
    private Long version = 0L;

    protected PaymentAttempt() {
    }

    public PaymentAttempt(Long bookingId, Long customerId, BigDecimal amount,
                          PaymentMode mode, String idempotencyKey) {
        this.bookingId = bookingId;
        this.customerId = customerId;
        this.amount = amount;
        this.mode = mode;
        this.idempotencyKey = idempotencyKey;
        this.createdAt = Instant.now();
    }

    /** SUCCEEDED — appelé après validation du paiement (mode simulé ou webhook prestataire). */
    public void markSucceeded(String providerReference, Instant when) {
        if (status != PaymentStatus.PENDING) {
            throw new IllegalStateException("Cannot succeed a " + status + " payment");
        }
        this.status = PaymentStatus.SUCCEEDED;
        this.providerReference = providerReference;
        this.processedAt = when;
    }

    /** FAILED — échec du paiement. */
    public void markFailed(String reason, Instant when) {
        if (status != PaymentStatus.PENDING) {
            throw new IllegalStateException("Cannot fail a " + status + " payment");
        }
        this.status = PaymentStatus.FAILED;
        this.failureReason = reason;
        this.processedAt = when;
    }

    /** CANCELLED — annulation par le client avant exécution. */
    public void markCancelled(Instant when) {
        if (status != PaymentStatus.PENDING) {
            throw new IllegalStateException("Cannot cancel a " + status + " payment");
        }
        this.status = PaymentStatus.CANCELLED;
        this.processedAt = when;
    }

    // Getters
    public Long getId() { return id; }
    public Long getBookingId() { return bookingId; }
    public Long getCustomerId() { return customerId; }
    public BigDecimal getAmount() { return amount; }
    public String getCurrency() { return currency; }
    public PaymentStatus getStatus() { return status; }
    public PaymentMode getMode() { return mode; }
    public String getProviderReference() { return providerReference; }
    public String getIdempotencyKey() { return idempotencyKey; }
    public Instant getCreatedAt() { return createdAt; }
    public Instant getProcessedAt() { return processedAt; }
    public String getFailureReason() { return failureReason; }
    public Long getVersion() { return version; }
}
