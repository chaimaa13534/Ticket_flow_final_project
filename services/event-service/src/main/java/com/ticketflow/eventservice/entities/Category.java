package com.ticketflow.eventservice.entities;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import java.time.Instant;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * Catégorie d'événement (concert, spectacle, sport, conférence...).
 * Le slug est unique et sert dans les URLs (FR-002 : filtre par catégorie).
 */
@Entity
@Table(name = "categories",
        indexes = {
                @Index(name = "uk_categories_slug", columnList = "slug", unique = true)
        })
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Category {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 80)
    private String name;

    /** Slug URL-friendly (ex : "concert-rock"). */
    @Column(nullable = false, unique = true, length = 100)
    private String slug;

    @Column(length = 500)
    private String description;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt = Instant.now();

    public Category(String name, String slug, String description) {
        this.name = name;
        this.slug = slug;
        this.description = description;
    }

    public void update(String name, String slug, String description) {
        this.name = name;
        this.slug = slug;
        this.description = description;
    }
}
