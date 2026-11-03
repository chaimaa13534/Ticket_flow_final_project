package com.ticketflow.aiassistantservice.repositories;

import com.ticketflow.aiassistantservice.entities.TelegramUpdateLog;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface TelegramUpdateLogRepo extends JpaRepository<TelegramUpdateLog, Long> {

    /** Déduplication par update_id Telegram. */
    Optional<TelegramUpdateLog> findByUpdateId(Long updateId);

    boolean existsByUpdateId(Long updateId);
}