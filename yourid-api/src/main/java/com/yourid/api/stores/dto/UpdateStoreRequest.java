package com.yourid.api.stores.dto;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class UpdateStoreRequest {
    private String name;
    private String description;
    private String logo;
    private String banner;
    private String primaryColor;
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
    private Boolean announcementActive;
    private String defaultCurrency;
}
