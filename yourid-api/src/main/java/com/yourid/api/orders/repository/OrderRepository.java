package com.yourid.api.orders.repository;

import com.yourid.api.orders.entity.Order;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface OrderRepository extends JpaRepository<Order, UUID> {
    Optional<Order> findByOrderNumber(String orderNumber);
    Page<Order> findByStoreId(UUID storeId, Pageable pageable);
    boolean existsByOrderNumber(String orderNumber);
}
