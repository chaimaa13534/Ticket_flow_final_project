package com.ticketflow.eventservice.repositories;

import com.ticketflow.eventservice.entities.Category;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface CategoryRepo extends JpaRepository<Category, Long> {

    Optional<Category> findBySlug(String slug);

    Optional<Category> findByNameIgnoreCase(String name);

    boolean existsBySlug(String slug);
}
