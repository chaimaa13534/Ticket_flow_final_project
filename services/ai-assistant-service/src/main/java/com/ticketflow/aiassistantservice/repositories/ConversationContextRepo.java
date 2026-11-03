package com.ticketflow.aiassistantservice.repositories;

import com.ticketflow.aiassistantservice.entities.ConversationContext;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface ConversationContextRepo extends JpaRepository<ConversationContext, Long> {

    Optional<ConversationContext> findByTelegramUserId(String telegramUserId);

    /** Recherche par token opaque (la PK numérique n'est jamais exposée hors service). */
    Optional<ConversationContext> findByConversationToken(String conversationToken);

    /** Contextes expirés (cron de nettoyage). */
    List<ConversationContext> findByExpiresAtBefore(Instant when);

    /**
     * Nettoyage bulk des contextes expirés (FR-024 : conservation limitée).
     *
     * IMPORTANT : les audits (tool_audit_logs) NE sont PAS supprimés — il n'y a AUCUNE
     * FK vers conversation_contexts, juste un UUID stocké pour référence. Les audits
     * restent pour auditabilité (CdC FR-020).
     */
    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("DELETE FROM ConversationContext c WHERE c.expiresAt < :when")
    int deleteExpired(@Param("when") Instant when);
}
