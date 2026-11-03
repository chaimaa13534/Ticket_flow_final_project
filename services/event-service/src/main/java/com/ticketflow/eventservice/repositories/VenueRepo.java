package com.ticketflow.eventservice.repositories;

import com.ticketflow.eventservice.entities.Venue;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface VenueRepo extends JpaRepository<Venue, Long> {

    List<Venue> findByCityIgnoreCaseOrderByCityAscNameAsc(String city);

    List<Venue> findAllByOrderByCityAscNameAsc();
}
