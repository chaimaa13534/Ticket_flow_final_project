package com.ticketflow.paymentservice.entities;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.time.Instant;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * Remboursement lié à une tentative de paiement (CdC FR-012).
 *
 * Un remboursement peut être partiel (PARTIAL) ou total (FULL).
 * Plusieurs remboursements partiels peuvent être émis pour une même tentative, tant
 * que leur somme ne dépasse pas le montant initial.
 */
@Entity
@Table(name = "refunds",
        indexes = {
                @Index(name = "idx_refunds_payment", columnList = "payment_attempt_id"),
                @Index(name = "idx_refunds_status", columnList = "status"),
                @Index(name = "idx_refunds_provider_ref", columnList = "provider_reference")
        })
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Refund {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(optional = false, fetch = FetchType.LAZY)
    @JoinColumn(name = "payment_attempt_id", nullable = false)
    private PaymentAttempt paymentAttempt;

    @Column(nullable = false, precision = 12, scale = 2)
    private BigDecimal amount;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 10)
    private RefundType type;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 15)
    private RefundStatus status = RefundStatus.PENDING;

    @Column(name = "provider_reference", length = 100)
    private String providerReference;

    @Column(length = 500)
    private String reason;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt = Instant.now();

    @Column(name = "processed_at")
    private Instant processedAt;

    public Refund(PaymentAttempt paymentAttempt, BigDecimal amount, RefundType type, String reason) {
        this.paymentAttempt = paymentAttempt;
        this.amount = amount;
        this.type = type;
        this.reason = reason;
    }

    public void markSucceeded(String providerReference, Instant when) {
        this.status = RefundStatus.SUCCEEDED;
        this.providerReference = providerReference;
        this.processedAt = when;
    }

    public void markFailed(Instant when) {
        this.status = RefundStatus.FAILED;
        this.processedAt = when;
    }

    public enum RefundType { PARTIAL, FULL }
    public enum RefundStatus { PENDING, SUCCEEDED, FAILED }
}