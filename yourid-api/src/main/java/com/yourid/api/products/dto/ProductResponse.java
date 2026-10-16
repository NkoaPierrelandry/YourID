package com.yourid.api.products.dto;

import com.yourid.api.products.enums.ContentType;
import com.yourid.api.products.enums.ProductStatus;
import com.yourid.api.products.enums.ProductType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

@Getter
@Builder
@AllArgsConstructor
public class ProductResponse {
    private UUID id;
    private UUID storeId;
    private String storeName;
    private UUID categoryId;
    private String categoryName;
    private ProductType type;
    private ProductStatus status;
    private String title;
    private String slug;
    private String description;
    private String shortDesc;
    private String coverImage;
    private BigDecimal price;
    private BigDecimal comparePrice;
    private String currency;
    private String contentUrl;
    private ContentType contentType;
    private BigDecimal rating;
    private int reviewCount;
    private int totalSales;
    private int viewCount;
    private String contentFileName;
    private Instant createdAt;
}