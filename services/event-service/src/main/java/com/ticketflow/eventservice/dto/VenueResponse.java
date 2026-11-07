package com.ticketflow.eventservice.dto;

import com.ticketflow.eventservice.entities.Venue;
import java.math.BigDecimal;

public record VenueResponse(
        Long id,
        String name,
        String address,
        String city,
        int capacity,
        BigDecimal latitude,
        BigDecimal longitude
) {
    public static VenueResponse from(Venue v) {
        return new VenueResponse(v.getId(), v.getName(), v.getAddress(), v.getCity(),
                v.getCapacity(), v.getLatitude(), v.getLongitude());
    }
}
