package com.yourid.api.reviews.service;

import com.yourid.api.common.exception.ResourceNotFoundException;
import com.yourid.api.products.entity.Product;
import com.yourid.api.products.repository.ProductRepository;
import com.yourid.api.reviews.dto.CreateReviewRequest;
import com.yourid.api.reviews.dto.ReviewResponse;
import com.yourid.api.reviews.entity.Review;
import com.yourid.api.reviews.repository.ReviewRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class ReviewService {

    private final ReviewRepository reviewRepository;
    private final ProductRepository productRepository;

    // Recalcule la note moyenne du produit a chaque nouvel avis, plutot que de la
    // stocker de maniere approximative -- garantit la coherence entre rating et review_count.
    @Transactional
    public ReviewResponse create(CreateReviewRequest request) {
        Product product = productRepository.findById(request.getProductId())
                .orElseThrow(() -> new ResourceNotFoundException("Produit introuvable."));

        Review review = Review.builder()
                .store(product.getStore())
                .product(product)
                .rating(request.getRating())
                .title(request.getTitle())
                .comment(request.getComment())
                .build();
        review = reviewRepository.save(review);

        recalculateRating(product, request.getRating());

        return toResponse(review);
    }

    public Page<ReviewResponse> listForProduct(UUID productId, Pageable pageable) {
        return reviewRepository.findByProductId(productId, pageable).map(this::toResponse);
    }

    // Moyenne ponderee : (moyenne actuelle * ancien nombre d avis + nouvelle note) / nouveau nombre.
    // newRating doit etre passe separement car il n est pas encore reflete dans product.getRating().
    private void recalculateRating(Product product, int newRating) {
        int oldCount = product.getReviewCount();
        int newCount = oldCount + 1;

        BigDecimal previousTotal = product.getRating().multiply(BigDecimal.valueOf(oldCount));
        BigDecimal updatedAverage = previousTotal
                .add(BigDecimal.valueOf(newRating))
                .divide(BigDecimal.valueOf(newCount), 2, RoundingMode.HALF_UP);

        product.setRating(updatedAverage);
        product.setReviewCount(newCount);
        productRepository.save(product);
    }

    private ReviewResponse toResponse(Review review) {
        return ReviewResponse.builder()
                .id(review.getId())
                .productId(review.getProduct().getId())
                .rating(review.getRating())
                .title(review.getTitle())
                .comment(review.getComment())
                .isVerified(review.isVerified())
                .createdAt(review.getCreatedAt())
                .build();
    }
}
