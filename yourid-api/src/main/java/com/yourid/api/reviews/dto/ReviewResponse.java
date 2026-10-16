package com.yourid.api.reviews.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

import java.time.Instant;
import java.util.UUID;

@Getter
@Builder
@AllArgsConstructor
public class ReviewResponse {
    private UUID id;
    private UUID productId;
    private int rating;
    private String title;
    private String comment;
    private boolean isVerified;
    private Instant createdAt;
}
