package com.ticketflow.customerservice.entities;

import jakarta.persistence.*;

import java.time.Instant;

/**
 * Préférences notifications du client (CdC FR-014 : préférences et désabonnements respectés).
 *
 * Partage la clé primaire avec Customer (1-1 strict) via {@code @MapsId}.
 *
 * Avantage vs. génération par séquence : pas de désynchronisation possible
 * entre customer.id et profile.id. Le profil du client 2 ne peut pas s'attacher
 * au client 1.
 */
@Entity
@Table(name = "customer_profiles")
public class CustomerProfile {

    /** PK = customer.id (partagée, jamais générée par une séquence). */
    @Id
    private Long id;

    @OneToOne
    @JoinColumn(name = "id") // Dit à Hibernate que la FK est la PK (colonne "id")
    @MapsId  // id = customer.id automatiquement
    private Customer customer;

    @Column(name = "email_enabled", nullable = false)
    private boolean emailEnabled = true;

    @Column(name = "telegram_enabled", nullable = false)
    private boolean telegramEnabled = false;

    @Column(name = "marketing_opt_in", nullable = false)
    private boolean marketingOptIn = false;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    protected CustomerProfile() {
    }

    public CustomerProfile(Customer customer) {
        this.customer = customer;
    }

    @PrePersist
    void onCreate() {
        this.updatedAt = Instant.now();
    }

    @PreUpdate
    void onUpdate() {
        this.updatedAt = Instant.now();
    }

    // Getters
    public Long getId() { return id; }
    public Customer getCustomer() { return customer; }
    public boolean isEmailEnabled() { return emailEnabled; }
    public boolean isTelegramEnabled() { return telegramEnabled; }
    public boolean isMarketingOptIn() { return marketingOptIn; }
    public Instant getUpdatedAt() { return updatedAt; }

    // Setters (limités aux préférences — pas de setter sur customer)
    public void setEmailEnabled(boolean emailEnabled) { this.emailEnabled = emailEnabled; }
    public void setTelegramEnabled(boolean telegramEnabled) { this.telegramEnabled = telegramEnabled; }
    public void setMarketingOptIn(boolean marketingOptIn) { this.marketingOptIn = marketingOptIn; }
}

