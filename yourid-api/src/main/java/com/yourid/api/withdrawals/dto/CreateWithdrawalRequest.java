package com.yourid.api.withdrawals.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.util.UUID;

@Getter
@Setter
public class CreateWithdrawalRequest {

    @NotNull(message = "Le montant est requis.")
    @DecimalMin(value = "0.01", message = "Le montant doit etre positif.")
    private BigDecimal amount;

    @NotNull(message = "Le compte de paiement est requis.")
    private UUID paymentAccountId;

    private String notes;
}