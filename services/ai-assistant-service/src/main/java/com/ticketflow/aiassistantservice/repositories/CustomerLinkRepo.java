package com.ticketflow.aiassistantservice.repositories;

import com.ticketflow.aiassistantservice.entities.CustomerLink;
import com.ticketflow.aiassistantservice.enums.TelegramLinkStatus;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface CustomerLinkRepo extends JpaRepository<CustomerLink, Long> {

    /**
     * Retourne la liaison VERIFIED d'un user Telegram.
     * L'unicité est PARTIELLE côté BDD : seulement sur status='VERIFIED', donc
     * une nouvelle liaison peut être créée après REVOKED (FR-017).
     */
    Optional<CustomerLink> findByTelegramUserIdAndStatus(String telegramUserId, TelegramLinkStatus status);

    Optional<CustomerLink> findByCustomerIdAndStatus(Long customerId, TelegramLinkStatus status);
}
