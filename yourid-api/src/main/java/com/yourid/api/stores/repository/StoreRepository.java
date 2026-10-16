package com.yourid.api.stores.repository;

import com.yourid.api.stores.entity.Store;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface StoreRepository extends JpaRepository<Store, UUID> {
    Optional<Store> findByUserId(UUID userId);
    Optional<Store> findBySlug(String slug);
    boolean existsBySlug(String slug);
}
