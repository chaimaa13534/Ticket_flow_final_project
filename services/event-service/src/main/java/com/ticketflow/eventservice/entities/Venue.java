package com.ticketflow.eventservice.entities;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.time.Instant;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * Lieu physique d'un événement (CdC §7.1 : Venue).
 *
 * Séparé de Event car :
 *  - un lieu peut être réutilisé pour plusieurs événements ;
 *  - les informations géographiques (ville, GPS) servent au filtrage du catalogue (FR-002).
 */
@Entity
@Table(name = "venues",
        indexes = @Index(name = "idx_venues_city", columnList = "city"))
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Venue {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 150)
    private String name;

    @Column(nullable = false, length = 300)
    private String address;

    @Column(nullable = false, length = 100)
    private String city;

    @Column(nullable = false)
    private int capacity;

    /** Latitude en degrés décimaux (null si non géocodé). */
    @Column(precision = 9, scale = 6)
    private BigDecimal latitude;

    @Column(precision = 9, scale = 6)
    private BigDecimal longitude;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt = Instant.now();

    public Venue(String name, String address, String city, int capacity,
                 BigDecimal latitude, BigDecimal longitude) {
        this.name = name;
        this.address = address;
        this.city = city;
        this.capacity = capacity;
        this.latitude = latitude;
        this.longitude = longitude;
    }

    public void update(String name, String address, String city, int capacity,
                       BigDecimal latitude, BigDecimal longitude) {
        this.name = name;
        this.address = address;
        this.city = city;
        this.capacity = capacity;
        this.latitude = latitude;
        this.longitude = longitude;
    }
}
