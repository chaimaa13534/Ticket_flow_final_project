package com.ticketflow.aiassistantservice.repositories;

import com.ticketflow.aiassistantservice.entities.ToolAuditLog;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface ToolAuditLogRepo extends JpaRepository<ToolAuditLog, Long> {
}