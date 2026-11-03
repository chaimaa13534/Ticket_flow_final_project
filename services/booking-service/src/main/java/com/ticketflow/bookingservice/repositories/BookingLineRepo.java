package com.ticketflow.bookingservice.repositories;

import com.ticketflow.bookingservice.entities.BookingLine;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface BookingLineRepo extends JpaRepository<BookingLine, Long> {

    List<BookingLine> findByBookingId(Long bookingId);
}
