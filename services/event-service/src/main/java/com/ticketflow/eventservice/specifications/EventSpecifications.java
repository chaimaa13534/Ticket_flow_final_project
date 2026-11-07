package com.ticketflow.eventservice.specifications;

import com.ticketflow.eventservice.entities.Event;
import com.ticketflow.eventservice.entities.EventStatus;
import jakarta.persistence.criteria.Predicate;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import org.springframework.data.jpa.domain.Specification;

/**
 * Spécifications dynamiques pour la recherche d'événements publics (FR-002).
 *
 * Avantage vs JPQL `(:param IS NULL OR ...)` : on n'ajoute QUE les critères réellement
 * fournis par le client. PostgreSQL reçoit une requête propre, sans paramètres nuls
 * qui désactivent les index.
 */
public final class EventSpecifications {

    private EventSpecifications() {}

    public static Specification<Event> isPublished() {
        return (root, query, cb) -> cb.equal(root.get("status"), EventStatus.PUBLISHED);
    }

    public static Specification<Event> withFilters(
            String text, String city, Long categoryId,
            Instant dateFrom, Instant dateTo,
            BigDecimal minPrice, BigDecimal maxPrice) {

        return (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();
            predicates.add(cb.equal(root.get("status"), EventStatus.PUBLISHED));

            if (text != null && !text.isBlank()) {
                String pattern = "%" + text.toLowerCase() + "%";
                predicates.add(cb.or(
                        cb.like(cb.lower(root.get("title")), pattern),
                        cb.like(cb.lower(root.get("description")), pattern)
                ));
            }
            if (city != null && !city.isBlank()) {
                predicates.add(cb.equal(cb.lower(root.get("venue").get("city")),
                        city.toLowerCase()));
            }
            if (categoryId != null) {
                predicates.add(cb.equal(root.get("category").get("id"), categoryId));
            }
            if (dateFrom != null) {
                predicates.add(cb.greaterThanOrEqualTo(root.get("startsAt"), dateFrom));
            }
            if (dateTo != null) {
                predicates.add(cb.lessThanOrEqualTo(root.get("startsAt"), dateTo));
            }
            if (minPrice != null) {
                predicates.add(cb.greaterThanOrEqualTo(root.get("basePrice"), minPrice));
            }
            if (maxPrice != null) {
                predicates.add(cb.lessThanOrEqualTo(root.get("basePrice"), maxPrice));
            }

            // Empêche le comptage de l'organisateur / venue de générer N+1
            query.distinct(true);
            return cb.and(predicates.toArray(new Predicate[0]));
        };
    }
}
