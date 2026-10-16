package com.yourid.api.withdrawals.service;

import com.yourid.api.common.exception.BadRequestException;
import com.yourid.api.common.exception.ResourceNotFoundException;
import com.yourid.api.notifications.service.NotificationService;
import com.yourid.api.stores.entity.Store;
import com.yourid.api.stores.repository.StoreRepository;
import com.yourid.api.stores.service.StoreService;
import com.yourid.api.withdrawals.dto.CreateWithdrawalRequest;
import com.yourid.api.withdrawals.dto.RejectWithdrawalRequest;
import com.yourid.api.withdrawals.dto.WithdrawalResponse;
import com.yourid.api.withdrawals.entity.Withdrawal;
import com.yourid.api.withdrawals.enums.WithdrawalStatus;
import com.yourid.api.withdrawals.repository.WithdrawalRepository;
import com.yourid.api.withdrawals.entity.PaymentAccount;
import com.yourid.api.withdrawals.repository.PaymentAccountRepository;

import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

//import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class WithdrawalService {

    private final WithdrawalRepository withdrawalRepository;
    private final StoreRepository storeRepository;
    private final StoreService storeService;
    private final NotificationService notificationService;
    private final PaymentAccountRepository paymentAccountRepository; 
        
    @Transactional
    public WithdrawalResponse create(UUID userId, CreateWithdrawalRequest request) {
        Store store = storeService.findByUserId(userId);

        if (store.getBalance().compareTo(request.getAmount()) < 0) {
            throw new BadRequestException("Solde insuffisant pour ce retrait.");
        }

        PaymentAccount account = paymentAccountRepository.findByIdAndStoreId(request.getPaymentAccountId(), store.getId())
                .orElseThrow(() -> new ResourceNotFoundException("Compte de paiement introuvable."));

        Withdrawal withdrawal = Withdrawal.builder()
                .store(store)
                .paymentAccount(account)
                .amount(request.getAmount())
                .currency(store.getCurrency())
                .method(account.getMethod().name())
                .accountInfo(account.getAccountInfo())
                .notes(request.getNotes())
                .build();

        return toResponse(withdrawalRepository.save(withdrawal));
    }

    public Page<WithdrawalResponse> listMine(UUID userId, Pageable pageable) {
        Store store = storeService.findByUserId(userId);
        return withdrawalRepository.findByStoreId(store.getId(), pageable).map(this::toResponse);
    }

    @Transactional
    public WithdrawalResponse approve(UUID withdrawalId) {
        Withdrawal withdrawal = findEntity(withdrawalId);
        assertPending(withdrawal);
        withdrawal.setStatus(WithdrawalStatus.APPROVED);
        return toResponse(withdrawalRepository.save(withdrawal));
    }

    @Transactional
    public WithdrawalResponse reject(UUID withdrawalId, RejectWithdrawalRequest request) {
        Withdrawal withdrawal = findEntity(withdrawalId);
        assertPending(withdrawal);
        withdrawal.setStatus(WithdrawalStatus.REJECTED);
        withdrawal.setAdminNote(request.getAdminNote());
        withdrawal = withdrawalRepository.save(withdrawal);

        notificationService.notify(
                withdrawal.getStore().getUser(),
                "withdrawal_rejected",
                "Retrait rejete",
                "Votre demande de retrait de " + withdrawal.getAmount() + " " + withdrawal.getCurrency() + " a ete rejetee : " + request.getAdminNote()
        );
        return toResponse(withdrawal);
    }

    // Marque le retrait comme paye et debite reellement le solde de la boutique.
    @Transactional
    public WithdrawalResponse markPaid(UUID withdrawalId) {
        Withdrawal withdrawal = findEntity(withdrawalId);
        if (withdrawal.getStatus() != WithdrawalStatus.APPROVED) {
            throw new BadRequestException("Seul un retrait APPROVED peut etre marque comme paye.");
        }

        Store store = withdrawal.getStore();
        if (store.getBalance().compareTo(withdrawal.getAmount()) < 0) {
            throw new BadRequestException("Solde insuffisant au moment du paiement.");
        }
        store.setBalance(store.getBalance().subtract(withdrawal.getAmount()));
        storeRepository.save(store);

        withdrawal.setStatus(WithdrawalStatus.PAID);
        withdrawal.setProcessedAt(Instant.now());
        withdrawal = withdrawalRepository.save(withdrawal);

        notificationService.notify(
                store.getUser(),
                "withdrawal_paid",
                "Retrait effectue",
                "Votre retrait de " + withdrawal.getAmount() + " " + withdrawal.getCurrency() + " a ete envoye."
        );
        return toResponse(withdrawal);
    }

    private void assertPending(Withdrawal withdrawal) {
        if (!withdrawal.isPending()) {
            throw new BadRequestException("Ce retrait a deja ete traite.");
        }
    }

    private Withdrawal findEntity(UUID id) {
        return withdrawalRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Retrait introuvable."));
    }

    private WithdrawalResponse toResponse(Withdrawal withdrawal) {
        return WithdrawalResponse.builder()
                .id(withdrawal.getId())
                .amount(withdrawal.getAmount())
                .currency(withdrawal.getCurrency())
                .status(withdrawal.getStatus())
                .method(withdrawal.getMethod())
                .notes(withdrawal.getNotes())
                .adminNote(withdrawal.getAdminNote())
                .processedAt(withdrawal.getProcessedAt())
                .createdAt(withdrawal.getCreatedAt())
                .paymentAccountLabel(withdrawal.getPaymentAccount() != null ? withdrawal.getPaymentAccount().getLabel() : null)
                .build();
    }
}
