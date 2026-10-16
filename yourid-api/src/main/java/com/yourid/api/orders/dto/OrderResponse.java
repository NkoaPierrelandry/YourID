package com.yourid.api.orders.dto;

import com.yourid.api.orders.enums.OrderStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Getter
@Builder
@AllArgsConstructor
public class OrderResponse {
    private UUID id;
    private String orderNumber;
    private OrderStatus status;
    private BigDecimal subtotal;
    private BigDecimal platformFee;
    private BigDecimal sellerRevenue;
    private BigDecimal total;
    private String currency;
    private String customerEmail;
    private String customerName;
    private List<OrderItemResponse> items;
    private Instant completedAt;
    private Instant createdAt;
}
