package com.yourid.api.reviews.controller;

import com.yourid.api.common.response.ApiResponse;
import com.yourid.api.reviews.dto.CreateReviewRequest;
import com.yourid.api.reviews.dto.ReviewResponse;
import com.yourid.api.reviews.service.ReviewService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/reviews")
@RequiredArgsConstructor
public class ReviewController {

    private final ReviewService reviewService;

    // Public : laisse volontairement ouvert car un acheteur na pas de compte utilisateur
    // (achat via email uniquement) -- amelioration possible : exiger une preuve d achat
    // (order_number) avant d accepter l avis.
    @PostMapping
    public ResponseEntity<ApiResponse<ReviewResponse>> create(@Valid @RequestBody CreateReviewRequest request) {
        ReviewResponse response = reviewService.create(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.ok(response, "Avis publie."));
    }

    @GetMapping("/product/{productId}")
    public ApiResponse<Page<ReviewResponse>> listForProduct(@PathVariable UUID productId, Pageable pageable) {
        return ApiResponse.ok(reviewService.listForProduct(productId, pageable));
    }
}
