package com.ticketflow.bookingservice.repositories;

import com.ticketflow.bookingservice.entities.IdempotencyKey;
import java.time.Instant;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface IdempotencyKeyRepo extends JpaRepository<IdempotencyKey, Long> {

    /** Unicité (customerId, idempotencyKey) garantie côté BDD via index unique. */
    Optional<IdempotencyKey> findByCustomerIdAndIdempotencyKey(Long customerId, String idempotencyKey);

    /**
     * Nettoyage périodique des clés expirées (cron, FR-008).
     *
     * clearAutomatically + flushAutomatically : éviter que des entités périmées restent
     * dans le contexte de persistance après le DELETE.
     */
    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("DELETE FROM IdempotencyKey k WHERE k.expiresAt < :now")
    int deleteExpired(@Param("now") Instant now);
}