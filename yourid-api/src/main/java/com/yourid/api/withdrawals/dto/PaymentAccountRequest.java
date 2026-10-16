package com.yourid.api.withdrawals.dto;

import com.yourid.api.withdrawals.enums.PaymentMethod;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class PaymentAccountRequest {

    @NotNull(message = "La methode de paiement est requise.")
    private PaymentMethod method;

    private String label;

    @NotBlank(message = "Les informations du compte sont requises.")
    private String accountInfo;
}