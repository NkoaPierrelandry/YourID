package com.yourid.api.stores.dto;

import com.yourid.api.stores.enums.StorePlan;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

@Getter
@Builder
@AllArgsConstructor
public class StoreResponse {
    private UUID id;
    private String name;
    private String slug;
    private String description;
    private String logo;
    private String banner;
    private String primaryColor;
    private StorePlan plan;
    private String currency;
    private String country;
    private String website;
    private String twitter;
    private String instagram;
    private String youtube;
    private String tiktok;
    private String facebook;
    private String tagline;
    private String contactEmail;
    private String contactPhone;
    private String announcement;
    private boolean announcementActive;
    private String defaultCurrency;
    private boolean isVerified;
    private BigDecimal totalRevenue;
    private int totalSales;
    private BigDecimal balance;
    private Instant createdAt;
}
