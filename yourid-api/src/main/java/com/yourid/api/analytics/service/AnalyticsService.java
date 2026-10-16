package com.yourid.api.analytics.service;

import com.yourid.api.analytics.dto.DashboardResponse;
import com.yourid.api.analytics.dto.TrackEventRequest;
import com.yourid.api.analytics.entity.AnalyticsEvent;
import com.yourid.api.analytics.repository.AnalyticsEventRepository;
import com.yourid.api.orders.repository.OrderRepository;
import com.yourid.api.products.repository.ProductRepository;
import com.yourid.api.stores.entity.Store;
import com.yourid.api.stores.service.StoreService;
import com.yourid.api.withdrawals.enums.WithdrawalStatus;
import com.yourid.api.withdrawals.repository.WithdrawalRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
@RequiredArgsConstructor
public class AnalyticsService {

    private final AnalyticsEventRepository analyticsEventRepository;
    private final StoreService storeService;
    private final ProductRepository productRepository;
    private final OrderRepository orderRepository;
    private final WithdrawalRepository withdrawalRepository;

    // Pas de FK stricte volontairement (voir schema) -- un evenement peut etre trace
    // meme pour une page anonyme sans storeId/productId.
    @Transactional
    public void track(TrackEventRequest request, String ipAddress) {
        AnalyticsEvent event = AnalyticsEvent.builder()
                .storeId(request.getStoreId())
                .productId(request.getProductId())
                .event(request.getEvent())
                .properties(request.getProperties())
                .ipAddress(ipAddress)
                .build();
        analyticsEventRepository.save(event);
    }

    public DashboardResponse getDashboard(UUID userId) {
        Store store = storeService.findByUserId(userId);

        long productCount = productRepository.findByStoreId(store.getId(), PageRequest.of(0, 1)).getTotalElements();
        long orderCount = orderRepository.findByStoreId(store.getId(), PageRequest.of(0, 1)).getTotalElements();
        long pendingWithdrawals = withdrawalRepository.findByStoreId(store.getId(), PageRequest.of(0, 100))
                .stream()
                .filter(w -> w.getStatus() == WithdrawalStatus.PENDING)
                .count();

        return DashboardResponse.builder()
                .totalRevenue(store.getTotalRevenue())
                .balance(store.getBalance())
                .totalSales(store.getTotalSales())
                .productCount(productCount)
                .orderCount(orderCount)
                .pendingWithdrawalsCount(pendingWithdrawals)
                .build();
    }
}
