package com.ticketflow.eventservice.repositories;

import com.ticketflow.eventservice.entities.TicketTier;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface TicketTierRepo extends JpaRepository<TicketTier, Long> {

    List<TicketTier> findByEventIdOrderByPriceAsc(Long eventId);

    boolean existsByEventIdAndName(Long eventId, String name);
}
