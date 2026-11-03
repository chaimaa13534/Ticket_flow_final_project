package com.ticketflow.customerservice.entities;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import jakarta.persistence.Version;
import java.time.Instant;

/**
 * Compte client TicketFlow.
 *
 * Propriétaire : Customer Service (CdC §7.1).
 *
 * Le {@code keycloakId} est l'identifiant externe issu de Keycloak (sub du JWT).
 * Aucune duplication des credentials ici : Keycloak reste l'autorité d'authentification.
 * Ce service ne stocke que les données profil et le statut métier.
 *
 * {@code email} est unique et normalisé en minuscules avant persist.
 *
 * {@code @Version} active l'optimistic locking pour protéger les transitions de statut
 * (suspendre / réactiver / clôturer) contre les doubles clics admin.
 */
@Entity
@Table(name = "customers",
        indexes = {
                @Index(name = "idx_customers_email", columnList = "email", unique = true),
                @Index(name = "idx_customers_keycloak_id", columnList = "keycloak_id", unique = true)
        })
public class Customer {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** Sub du JWT Keycloak — unique. Permet la jointure logique avec l'identité Keycloak. */
    @Column(name = "keycloak_id", nullable = false, unique = true, length = 100)
    private String keycloakId;

    @Column(nullable = false, length = 100)
    private String firstName;

    @Column(nullable = false, length = 100)
    private String lastName;

    @Column(nullable = false, unique = true, length = 254)
    private String email;

    @Column(length = 30)
    private String phone;

    /** Locale préférée pour les notifications (FR, EN, AR...). */
    @Column(name = "preferred_locale", length = 10)
    private String preferredLocale = "fr";

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    private CustomerStatus status = CustomerStatus.ACTIVE;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    /** Optimistic locking : protège les transitions de statut contre la concurrence admin. */
    @Version
    @Column(name = "version", nullable = false)
    private Long version = 0L;

    /** JPA requiert un constructeur no-arg. */
    protected Customer() {
    }

    public Customer(String keycloakId, String firstName, String lastName, String email, String phone) {
        this.keycloakId = keycloakId;
        this.firstName = firstName;
        this.lastName = lastName;
        this.email = email;
        this.phone = phone;
    }

    @PrePersist
    void onCreate() {
        Instant now = Instant.now();
        this.createdAt = now;
        this.updatedAt = now;
    }

    @PreUpdate
    void onUpdate() {
        this.updatedAt = Instant.now();
    }

    public void update(String firstName, String lastName, String email, String phone) {
        this.firstName = firstName;
        this.lastName = lastName;
        this.email = email;
        this.phone = phone;
    }

    public void suspend() {
        this.status = CustomerStatus.SUSPENDED;
    }

    public void reactivate() {
        this.status = CustomerStatus.ACTIVE;
    }

    public void close() {
        this.status = CustomerStatus.CLOSED;
    }

    // --- Getters ---
    public Long getId() { return id; }
    public String getKeycloakId() { return keycloakId; }
    public void setKeycloakId(String keycloakId) { this.keycloakId = keycloakId; }
    public String getFirstName() { return firstName; }
    public String getLastName() { return lastName; }
    public String getEmail() { return email; }
    public String getPhone() { return phone; }
    public void setPhone(String phone) { this.phone = phone; }
    public String getPreferredLocale() { return preferredLocale; }
    public void setPreferredLocale(String preferredLocale) { this.preferredLocale = preferredLocale; }
    public CustomerStatus getStatus() { return status; }
    public Instant getCreatedAt() { return createdAt; }
    public Instant getUpdatedAt() { return updatedAt; }
    public Long getVersion() { return version; }
}
