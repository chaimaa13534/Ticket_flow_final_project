package com.ticketflow.notificationservice.repositories;

import com.ticketflow.notificationservice.entities.NotificationAttempt;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface NotificationAttemptRepo extends JpaRepository<NotificationAttempt, Long> {

    List<NotificationAttempt> findByRequest_IdOrderByAttemptNumberDesc(Long requestId);
}
