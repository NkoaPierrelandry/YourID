package com.yourid.api.stores.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class CreateStoreRequest {

    @NotBlank(message = "Le nom de la boutique est requis.")
    @Size(max = 150)
    private String name;

    private String description;
    private String currency;
    private String country;
}
