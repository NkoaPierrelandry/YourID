package com.yourid.api.orders.controller;

import com.yourid.api.common.response.ApiResponse;
import com.yourid.api.common.security.UserPrincipal;
import com.yourid.api.orders.dto.CompleteOrderRequest;
import com.yourid.api.orders.dto.CreateOrderRequest;
import com.yourid.api.orders.dto.OrderResponse;
import com.yourid.api.orders.service.OrderService;
import com.yourid.api.products.service.ProductService;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.core.io.InputStreamResource;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.core.io.InputStreamResource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/orders")
@RequiredArgsConstructor
public class OrderController {

    private final OrderService orderService;

    // Public : un acheteur passe commande sans compte.
    @PostMapping("/checkout/{storeId}")
    public ResponseEntity<ApiResponse<OrderResponse>> checkout(
            @PathVariable UUID storeId,
            @Valid @RequestBody CreateOrderRequest request
    ) {
        OrderResponse response = orderService.checkout(storeId, request);
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.ok(response, "Commande creee, en attente de paiement."));
    }

    // Simule le webhook du fournisseur de paiement -- a securiser avant prod (voir OrderService).
    @PostMapping("/{orderNumber}/complete")
    public ApiResponse<OrderResponse> complete(
            @PathVariable String orderNumber,
            @Valid @RequestBody CompleteOrderRequest request
    ) {
        return ApiResponse.ok(orderService.completeOrder(orderNumber, request), "Commande confirmee.");
    }

    // Public : l acheteur suit sa commande / recupere ses liens de telechargement.
    @GetMapping("/{orderNumber}")
    public ApiResponse<OrderResponse> getByOrderNumber(@PathVariable String orderNumber) {
        return ApiResponse.ok(orderService.getByOrderNumber(orderNumber));
    }

    @GetMapping("/me")
    public ApiResponse<Page<OrderResponse>> listMine(@AuthenticationPrincipal UserPrincipal principal, Pageable pageable) {
        return ApiResponse.ok(orderService.listMine(principal.getId(), pageable));
    }

        @GetMapping("/{orderNumber}/download/{productId}")
    public ResponseEntity<InputStreamResource> downloadContent(
            @PathVariable String orderNumber,
            @PathVariable UUID productId
    ) {
        var dl = orderService.downloadForBuyer(orderNumber, productId);
        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType(dl.contentType()))
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + dl.fileName() + "\"")
                .body(new InputStreamResource(dl.stream()));
    }
}
