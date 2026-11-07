package com.ticketflow.eventservice.services;

import com.ticketflow.eventservice.dto.VenueRequest;
import com.ticketflow.eventservice.entities.Venue;
import com.ticketflow.eventservice.exceptions.ResourceNotFoundException;
import com.ticketflow.eventservice.repositories.VenueRepo;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
public class VenueService {

    private final VenueRepo repo;

    public VenueService(VenueRepo repo) { this.repo = repo; }

    public Venue create(VenueRequest req) {
        return repo.save(new Venue(req.name(), req.address(), req.city(),
                req.capacity(), req.latitude(), req.longitude()));
    }

    @Transactional(readOnly = true)
    public Venue get(Long id) {
        return repo.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Venue", String.valueOf(id)));
    }

    @Transactional(readOnly = true)
    public List<Venue> findAll() {
        return repo.findAllByOrderByCityAscNameAsc();
    }

    @Transactional(readOnly = true)
    public List<Venue> findByCity(String city) {
        return repo.findByCityIgnoreCaseOrderByCityAscNameAsc(city);
    }

    public Venue update(Long id, VenueRequest req) {
        Venue v = get(id);
        v.update(req.name(), req.address(), req.city(), req.capacity(),
                req.latitude(), req.longitude());
        return v;
    }

    public void delete(Long id) {
        repo.delete(get(id));
    }
}
