package com.yourid.api.categories.repository;

import com.yourid.api.categories.entity.Category;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface CategoryRepository extends JpaRepository<Category, UUID>, JpaSpecificationExecutor<Category> {
    List<Category> findAllByOrderBySortOrderAsc();
    Optional<Category> findBySlug(String slug);
}
