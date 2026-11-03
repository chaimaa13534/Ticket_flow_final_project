package com.ticketflow.customerservice.repositories;

import com.ticketflow.customerservice.entities.CustomerProfile;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface CustomerProfileRepo extends JpaRepository<CustomerProfile, Long> {

    Optional<CustomerProfile> findByCustomerId(Long customerId);
}
