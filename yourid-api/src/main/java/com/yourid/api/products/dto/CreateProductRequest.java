package com.yourid.api.products.dto;

import com.yourid.api.products.enums.ContentType;
import com.yourid.api.products.enums.ProductType;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.util.UUID;

@Getter
@Setter
public class CreateProductRequest {

    @NotBlank(message = "Le titre est requis.")
    private String title;

    private String description;
    private String shortDesc;
    private UUID categoryId;

    @NotNull(message = "Le type de produit est requis.")
    private ProductType type;

    @NotNull(message = "Le prix est requis.")
    @DecimalMin(value = "0.0", inclusive = true, message = "Le prix ne peut pas etre negatif.")
    private BigDecimal price;

    private BigDecimal comparePrice;
    private String currency;
    private String contentUrl;
    private ContentType contentType;
    private String contentNote;
}
