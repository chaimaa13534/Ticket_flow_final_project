package com.ticketflow.customerservice.repositories;

import com.ticketflow.customerservice.entities.TelegramLink;
import com.ticketflow.customerservice.enums.TelegramLinkStatus;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface TelegramLinkRepo extends JpaRepository<TelegramLink, Long> {

    /**
     * Retourne la liaison PENDING ou VERIFIED d'un client (index unique partiel côté BDD).
     * Renvoie empty si la liaison a été révoquée ou expirée — permet la re-liaison (FR-017).
     */
    Optional<TelegramLink> findByCustomerIdAndStatusIn(Long customerId,
                                                       java.util.List<TelegramLinkStatus> statuses);

    default Optional<TelegramLink> findActiveByCustomerId(Long customerId) {
        return findByCustomerIdAndStatusIn(customerId,
                java.util.List.of(TelegramLinkStatus.PENDING, TelegramLinkStatus.VERIFIED));
    }

    Optional<TelegramLink> findByLinkCode(String linkCode);

    /** Liaison active par telegramUserId (une seule VERIFIED par user Telegram). */
    Optional<TelegramLink> findByTelegramUserIdAndStatus(String telegramUserId, TelegramLinkStatus status);
}

