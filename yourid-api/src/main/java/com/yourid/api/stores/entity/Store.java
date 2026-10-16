package com.yourid.api.stores.entity;

import com.yourid.api.stores.enums.StorePlan;
import com.yourid.api.users.entity.User;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "stores")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Store {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false, unique = true, foreignKey = @ForeignKey(name = "fk_stores_user"))
    private User user;

    @Column(nullable = false, length = 150)
    private String name;

    @Column(nullable = false, unique = true, length = 150)
    private String slug;

    @Column(columnDefinition = "TEXT")
    private String description;

    @Column(length = 500)
    private String logo;

    @Column(length = 500)
    private String banner;

    @Column(name = "primary_color", nullable = false, length = 7)
    private String primaryColor;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    @Builder.Default
    private StorePlan plan = StorePlan.STARTER;

    @Column(nullable = false, length = 3)
    private String currency;

    @Column(nullable = false, length = 2)
    private String country;

    private String website;
    private String twitter;
    private String instagram;
    private String youtube;
    private String tiktok;
    private String facebook;

    // Parametres additionnels (migration V6)
    @Column(length = 200)
    private String tagline;

    @Column(name = "contact_email", length = 255)
    private String contactEmail;

    @Column(name = "contact_phone", length = 30)
    private String contactPhone;

    @Column(length = 300)
    private String announcement;

    @Column(name = "announcement_active", nullable = false)
    @Builder.Default
    private boolean announcementActive = false;

    @Column(name = "default_currency", length = 3)
    private String defaultCurrency;

    @Column(name = "is_verified", nullable = false)
    private boolean isVerified;

    @Column(name = "total_revenue", nullable = false, precision = 14, scale = 2)
    private BigDecimal totalRevenue;

    @Column(name = "total_sales", nullable = false)
    private int totalSales;

    @Column(nullable = false, precision = 14, scale = 2)
    private BigDecimal balance;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    @PrePersist
    protected void onCreate() {
        Instant now = Instant.now();
        this.createdAt = now;
        this.updatedAt = now;
        if (this.primaryColor == null) this.primaryColor = "#00A86B";
        if (this.plan == null) this.plan = StorePlan.STARTER;
        if (this.currency == null) this.currency = "XOF";
        if (this.country == null) this.country = "SN";
        if (this.totalRevenue == null) this.totalRevenue = BigDecimal.ZERO;
        if (this.balance == null) this.balance = BigDecimal.ZERO;
    }

    @PreUpdate
    protected void onUpdate() {
        this.updatedAt = Instant.now();
    }
}
