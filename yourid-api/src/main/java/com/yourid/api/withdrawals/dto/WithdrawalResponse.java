package com.yourid.api.withdrawals.dto;

import com.yourid.api.withdrawals.enums.WithdrawalStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

@Getter
@Builder
@AllArgsConstructor
public class WithdrawalResponse {
    private UUID id;
    private BigDecimal amount;
    private String currency;
    private WithdrawalStatus status;
    private String method;
    private String paymentAccountLabel;
    private String notes;
    private String adminNote;
    private Instant processedAt;
    private Instant createdAt;
}