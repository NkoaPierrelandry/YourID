package com.yourid.api.products.dto;

import com.yourid.api.products.enums.ContentType;
import com.yourid.api.products.enums.ProductStatus;
import com.yourid.api.products.enums.ProductType;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.util.UUID;

@Getter
@Setter
public class UpdateProductRequest {
    private String title;
    private String description;
    private String shortDesc;
    private UUID categoryId;
    private ProductType type;
    private ProductStatus status;
    private BigDecimal price;
    private BigDecimal comparePrice;
    private String coverImage;
    private String contentUrl;
    private ContentType contentType;
    private String contentNote;
}
