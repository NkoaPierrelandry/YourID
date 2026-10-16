package com.yourid.api.customers.repository;

import com.yourid.api.customers.entity.Customer;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface CustomerRepository extends JpaRepository<Customer, UUID> {
    Optional<Customer> findByStoreIdAndEmail(UUID storeId, String email);
}
