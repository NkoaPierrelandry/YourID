package com.yourid.api.withdrawals.repository;

import com.yourid.api.withdrawals.entity.Withdrawal;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface WithdrawalRepository extends JpaRepository<Withdrawal, UUID> {
    Page<Withdrawal> findByStoreId(UUID storeId, Pageable pageable);
}
