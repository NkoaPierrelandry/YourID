package com.yourid.api.marketplace.controller;

import com.yourid.api.common.response.ApiResponse;
import com.yourid.api.products.dto.ProductResponse;
import com.yourid.api.products.service.ProductService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

/**
 * Vitrine publique : parcours du catalogue tous vendeurs confondus,
 * equivalent du module marketplace.* du backend NestJS d origine.
 */
@RestController
@RequestMapping("/api/v1/marketplace")
@RequiredArgsConstructor
public class MarketplaceController {

    private final ProductService productService;

    @GetMapping("/products")
    public ApiResponse<Page<ProductResponse>> browse(Pageable pageable) {
        return ApiResponse.ok(productService.listMarketplace(pageable));
    }

    @GetMapping("/featured")
    public ApiResponse<Page<ProductResponse>> featured(Pageable pageable) {
        return ApiResponse.ok(productService.listFeatured(pageable));
    }

    @GetMapping("/category/{categoryId}")
    public ApiResponse<Page<ProductResponse>> byCategory(@PathVariable UUID categoryId, Pageable pageable) {
        return ApiResponse.ok(productService.listByCategory(categoryId, pageable));
    }
}
