package com.ticketflow.eventservice.services;

import com.ticketflow.eventservice.dto.CategoryRequest;
import com.ticketflow.eventservice.entities.Category;
import com.ticketflow.eventservice.exceptions.ResourceNotFoundException;
import com.ticketflow.eventservice.repositories.CategoryRepo;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
public class CategoryService {

    private final CategoryRepo repo;

    public CategoryService(CategoryRepo repo) { this.repo = repo; }

    public Category create(CategoryRequest req) {
        if (repo.existsBySlug(req.slug())) {
            throw new IllegalArgumentException("Slug already used: " + req.slug());
        }
        return repo.save(new Category(req.name(), req.slug(), req.description()));
    }

    @Transactional(readOnly = true)
    public Category get(Long id) {
        return repo.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Category", String.valueOf(id)));
    }

    @Transactional(readOnly = true)
    public Category getBySlug(String slug) {
        return repo.findBySlug(slug)
                .orElseThrow(() -> new ResourceNotFoundException("Category", slug));
    }

    @Transactional(readOnly = true)
    public List<Category> findAll() {
        return repo.findAll();
    }

    public Category update(Long id, CategoryRequest req) {
        Category c = get(id);
        c.update(req.name(), req.slug(), req.description());
        return c;
    }

    public void delete(Long id) {
        repo.delete(get(id));
    }
}
