package com.yourid.api.analytics.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.Setter;

import java.util.UUID;

@Getter
@Setter
public class TrackEventRequest {

    @NotBlank(message = "Le nom de l evenement est requis.")
    private String event;

    private UUID storeId;
    private UUID productId;
    // JSON libre serialise tel quel (ex: {"page":"/produit/x","referrer":"google"}).
    private String properties;
}
