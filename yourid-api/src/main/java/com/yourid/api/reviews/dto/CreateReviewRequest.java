package com.yourid.api.reviews.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

import java.util.UUID;

@Getter
@Setter
public class CreateReviewRequest {

    @NotNull(message = "Le produit est requis.")
    private UUID productId;

    @Min(value = 1, message = "La note doit etre entre 1 et 5.")
    @Max(value = 5, message = "La note doit etre entre 1 et 5.")
    private int rating;

    private String title;
    private String comment;
}
