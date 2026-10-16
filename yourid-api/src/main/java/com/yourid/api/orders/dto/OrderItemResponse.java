package com.yourid.api.orders.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

import java.math.BigDecimal;
import java.util.UUID;

@Getter
@Builder
@AllArgsConstructor
public class OrderItemResponse {
    private UUID productId;
    private String title;
    private BigDecimal price;
    private int quantity;
    // Rempli uniquement si la commande est COMPLETED -- c est le lien de telechargement/acces.
    private String contentUrl;
}
