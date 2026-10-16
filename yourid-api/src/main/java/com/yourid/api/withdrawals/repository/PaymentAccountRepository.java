package com.yourid.api.withdrawals.repository;

import com.yourid.api.withdrawals.entity.PaymentAccount;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface PaymentAccountRepository extends JpaRepository<PaymentAccount, UUID> {
    List<PaymentAccount> findByStoreIdOrderByCreatedAtDesc(UUID storeId);

    // Le filtre store_id est fait DANS la requete, pas apres coup en Java --
    // impossible de recuperer le compte d un autre vendeur meme en devinant son id.
    Optional<PaymentAccount> findByIdAndStoreId(UUID id, UUID storeId);
}