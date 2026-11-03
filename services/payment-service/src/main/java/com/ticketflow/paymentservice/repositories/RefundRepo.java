package com.ticketflow.paymentservice.repositories;

import com.ticketflow.paymentservice.entities.Refund;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface RefundRepo extends JpaRepository<Refund, Long> {

    List<Refund> findByPaymentAttemptIdOrderByCreatedAtDesc(Long paymentAttemptId);
}
