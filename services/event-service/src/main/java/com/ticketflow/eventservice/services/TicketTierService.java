package com.ticketflow.eventservice.services;

import com.ticketflow.eventservice.dto.TicketTierRequest;
import com.ticketflow.eventservice.entities.Event;
import com.ticketflow.eventservice.entities.TicketTier;
import com.ticketflow.eventservice.exceptions.ResourceNotFoundException;
import com.ticketflow.eventservice.repositories.TicketTierRepo;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
public class TicketTierService {

    private final TicketTierRepo repo;
    private final EventService eventService;

    public TicketTierService(TicketTierRepo repo, EventService eventService) {
        this.repo = repo;
        this.eventService = eventService;
    }

    public TicketTier create(TicketTierRequest req) {
        Event event = eventService.get(req.eventId());
        if (repo.existsByEventIdAndName(req.eventId(), req.name())) {
            throw new IllegalArgumentException(
                    "Tier with name '" + req.name() + "' already exists for this event");
        }
        return repo.save(new TicketTier(event, req.name(), req.price(), req.quantityAvailable()));
    }

    @Transactional(readOnly = true)
    public TicketTier get(Long id) {
        return repo.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("TicketTier", String.valueOf(id)));
    }

    @Transactional(readOnly = true)
    public List<TicketTier> findByEvent(Long eventId) {
        return repo.findByEventIdOrderByPriceAsc(eventId);
    }

    public TicketTier update(Long id, TicketTierRequest req) {
        TicketTier tier = get(id);
        tier.update(req.name(), req.price(), req.quantityAvailable());
        return tier;
    }

    public void delete(Long id) {
        repo.delete(get(id));
    }
}
