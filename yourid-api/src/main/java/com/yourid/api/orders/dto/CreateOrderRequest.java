package com.yourid.api.orders.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import lombok.Getter;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
public class CreateOrderRequest {

    @NotBlank(message = "L email du client est requis.")
    @Email(message = "Format d email invalide.")
    private String customerEmail;

    @NotBlank(message = "Le prenom du client est requis.")
    private String customerFirstName;

    @NotBlank(message = "Le nom du client est requis.")
    private String customerLastName;

    @NotEmpty(message = "La commande doit contenir au moins un article.")
    @Valid
    private List<OrderItemRequest> items;
}
