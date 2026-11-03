package com.ticketflow.bookingservice.entities;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import java.time.Instant;

/**
 * Trace d'idempotency (CdC §7.1 et FR-008).
 *
 * Quand un client envoie {@code Idempotency-Key}, on cherche d'abord ici :
 *  - Si une entrée existe pour (customerId, idempotencyKey), on renvoie le booking associé.
 *  - Sinon, on crée le booking et on persiste la clé.
 *
 * Permet aux retries réseau (timeout client, double clic) de ne JAMAIS créer de double
 * réservation, conformément au CdC §7.1 : "idempotency key".
 *
 * Unicité : (customerId, idempotencyKey) — imposée côté BDD (index unique).
 */
@Entity
@Table(name = "idempotency_keys",
        indexes = {
                @Index(name = "uk_idem_key_customer", columnList = "customer_id, idempotency_key", unique = true),
                @Index(name = "idx_idem_expires_at", columnList = "expires_at")
        })
public class IdempotencyKey {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "customer_id", nullable = false)
    private Long customerId;

    @Column(name = "idempotency_key", nullable = false, length = 100)
    private String idempotencyKey;

    @Column(name = "booking_id")
    private Long bookingId;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "expires_at", nullable = false)
    private Instant expiresAt;

    protected IdempotencyKey() {
    }

    public IdempotencyKey(Long customerId, String idempotencyKey, Instant createdAt, Instant expiresAt) {
        this.customerId = customerId;
        this.idempotencyKey = idempotencyKey;
        this.createdAt = createdAt;
        this.expiresAt = expiresAt;
    }

    /** Une fois le booking créé, on l'associe à la clé. */
    public void attachBooking(Long bookingId) {
        this.bookingId = bookingId;
    }

    // Getters
    public Long getId() { return id; }
    public Long getCustomerId() { return customerId; }
    public String getIdempotencyKey() { return idempotencyKey; }
    public Long getBookingId() { return bookingId; }
    public Instant getCreatedAt() { return createdAt; }
    public Instant getExpiresAt() { return expiresAt; }
}
