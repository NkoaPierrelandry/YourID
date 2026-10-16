package com.yourid.api.withdrawals.service;

import com.yourid.api.common.exception.ResourceNotFoundException;
import com.yourid.api.stores.entity.Store;
import com.yourid.api.stores.service.StoreService;
import com.yourid.api.withdrawals.dto.PaymentAccountRequest;
import com.yourid.api.withdrawals.dto.PaymentAccountResponse;
import com.yourid.api.withdrawals.entity.PaymentAccount;
import com.yourid.api.withdrawals.repository.PaymentAccountRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class PaymentAccountService {

    private final PaymentAccountRepository paymentAccountRepository;
    private final StoreService storeService;

    @Transactional
    public PaymentAccountResponse create(UUID userId, PaymentAccountRequest request) {
        Store store = storeService.findByUserId(userId);
        boolean isFirstAccount = paymentAccountRepository.findByStoreIdOrderByCreatedAtDesc(store.getId()).isEmpty();

        PaymentAccount account = PaymentAccount.builder()
                .store(store)
                .method(request.getMethod())
                .label(request.getLabel())
                .accountInfo(request.getAccountInfo())
                .isDefault(isFirstAccount) // le premier compte devient automatiquement le defaut
                .build();

        return toResponse(paymentAccountRepository.save(account));
    }

    @Transactional(readOnly = true)
    public List<PaymentAccountResponse> listMine(UUID userId) {
        Store store = storeService.findByUserId(userId);
        return paymentAccountRepository.findByStoreIdOrderByCreatedAtDesc(store.getId())
                .stream().map(this::toResponse).collect(Collectors.toList());
    }

    @Transactional
    public void delete(UUID userId, UUID accountId) {
        Store store = storeService.findByUserId(userId);
        PaymentAccount account = findOwned(store.getId(), accountId);
        paymentAccountRepository.delete(account);
    }

    @Transactional
    public PaymentAccountResponse setDefault(UUID userId, UUID accountId) {
        Store store = storeService.findByUserId(userId);
        PaymentAccount target = findOwned(store.getId(), accountId);

        // Un seul compte par defaut a la fois : on desactive tous les autres d abord.
        paymentAccountRepository.findByStoreIdOrderByCreatedAtDesc(store.getId()).forEach(acc -> {
            if (acc.isDefault() && !acc.getId().equals(accountId)) {
                acc.setDefault(false);
                paymentAccountRepository.save(acc);
            }
        });

        target.setDefault(true);
        return toResponse(paymentAccountRepository.save(target));
    }

    // findByIdAndStoreId filtre deja par store_id en base : si le compte existe mais
    // appartient a un autre vendeur, cette methode leve exactement la meme exception
    // "introuvable" que s il n existait pas du tout -- aucune fuite d information.
    private PaymentAccount findOwned(UUID storeId, UUID accountId) {
        return paymentAccountRepository.findByIdAndStoreId(accountId, storeId)
                .orElseThrow(() -> new ResourceNotFoundException("Compte de paiement introuvable."));
    }

    private PaymentAccountResponse toResponse(PaymentAccount account) {
        return PaymentAccountResponse.builder()
                .id(account.getId())
                .method(account.getMethod())
                .label(account.getLabel())
                .accountInfo(account.getAccountInfo())
                .isDefault(account.isDefault())
                .createdAt(account.getCreatedAt())
                .build();
    }
}