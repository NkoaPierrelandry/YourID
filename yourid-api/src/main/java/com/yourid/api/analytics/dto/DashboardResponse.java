package com.yourid.api.analytics.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

import java.math.BigDecimal;

@Getter
@Builder
@AllArgsConstructor
public class DashboardResponse {
    private BigDecimal totalRevenue;
    private BigDecimal balance;
    private int totalSales;
    private long productCount;
    private long orderCount;
    private long pendingWithdrawalsCount;
}
