package com.yourid.api.withdrawals.dto;

import com.yourid.api.withdrawals.enums.PaymentMethod;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

import java.time.Instant;
import java.util.UUID;

@Getter
@Builder
@AllArgsConstructor
public class PaymentAccountResponse {
    private UUID id;
    private PaymentMethod method;
    private String label;
    private String accountInfo;
    private boolean isDefault;
    private Instant createdAt;
}