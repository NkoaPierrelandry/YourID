package com.yourid.api.orders.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

import java.util.UUID;

@Getter
@Setter
public class OrderItemRequest {

    @NotNull(message = "L id du produit est requis.")
    private UUID productId;

    @Min(value = 1, message = "La quantite doit etre au moins 1.")
    private int quantity = 1;
}
