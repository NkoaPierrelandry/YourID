package com.yourid.api.payments.service;

import com.yourid.api.orders.entity.Order;
import com.yourid.api.payments.entity.Transaction;
import com.yourid.api.payments.enums.TransactionStatus;
import com.yourid.api.payments.repository.TransactionRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;

/**
 * Encapsule la creation/validation des Transaction associees a une Order.
 * Pas d integration reelle avec un fournisseur de paiement pour l instant --
 * a brancher plus tard (ex: PayDunya, Orange Money API...). En attendant,
 * completeOrder() joue le role d un "webhook" simule.
 */
@Service
@RequiredArgsConstructor
public class PaymentService {

    private final TransactionRepository transactionRepository;

    @Transactional
    public Transaction createPendingTransaction(Order order, String provider) {
        Transaction transaction = Transaction.builder()
                .order(order)
                .amount(order.getTotal())
                .currency(order.getCurrency())
                .provider(provider)
                .build();
        return transactionRepository.save(transaction);
    }

    @Transactional
    public Transaction markCompleted(Order order, String providerRef) {
        Transaction transaction = transactionRepository.findByOrderId(order.getId())
                .orElseThrow(() -> new IllegalStateException("Aucune transaction associee a la commande " + order.getId()));
        transaction.setStatus(TransactionStatus.COMPLETED);
        transaction.setProviderRef(providerRef);
        transaction.setProcessedAt(Instant.now());
        return transactionRepository.save(transaction);
    }
}
