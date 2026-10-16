package com.yourid.api.orders.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class CompleteOrderRequest {

    @NotBlank(message = "La methode de paiement est requise.")
    private String paymentMethod;

    private String paymentRef;

    @NotBlank(message = "Le fournisseur de paiement est requis.")
    private String provider;

    private String providerRef;
}
