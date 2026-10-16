package com.yourid.api.products.entity;

import com.yourid.api.categories.entity.Category;
import com.yourid.api.products.enums.ContentType;
import com.yourid.api.products.enums.ProductStatus;
import com.yourid.api.products.enums.ProductType;
import com.yourid.api.stores.entity.Store;

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
@Table(name = "products")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Product {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "store_id", nullable = false, foreignKey = @ForeignKey(name = "fk_products_store"))
    private Store store;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "category_id", foreignKey = @ForeignKey(name = "fk_products_category"))
    private Category category;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private ProductType type;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private ProductStatus status;

    @Column(nullable = false)
    private String title;

    @Column(nullable = false, unique = true)
    private String slug;

    @Column(columnDefinition = "TEXT")
    private String description;

    @Column(name = "short_desc", length = 500)
    private String shortDesc;

    @Column(name = "cover_image", length = 500)
    private String coverImage;

    @Column(nullable = false, precision = 12, scale = 2)
    private BigDecimal price;

    @Column(name = "compare_price", precision = 12, scale = 2)
    private BigDecimal comparePrice;

    @Column(nullable = false, length = 3)
    private String currency;

    @Column(name = "content_url", length = 1000)
    private String contentUrl;

    @Enumerated(EnumType.STRING)
    @Column(name = "content_type", length = 20)
    private ContentType contentType;

    @Column(name = "content_note", columnDefinition = "TEXT")
    private String contentNote;

    @Column(name = "content_file_path", length = 500)
    private String contentFilePath;

    @Column(name = "content_file_name")
    private String contentFileName;
    @Column(name = "is_featured", nullable = false)
    private boolean isFeatured;

    @Column(name = "is_marketplace", nullable = false)
    private boolean isMarketplace;

    @Column(name = "total_sales", nullable = false)
    private int totalSales;

    @Column(name = "total_revenue", nullable = false, precision = 14, scale = 2)
    private BigDecimal totalRevenue;

    @Column(nullable = false, precision = 3, scale = 2)
    private BigDecimal rating;

    @Column(name = "review_count", nullable = false)
    private int reviewCount;

    @Column(name = "view_count", nullable = false)
    private int viewCount;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    @PrePersist
    protected void onCreate() {
        Instant now = Instant.now();
        this.createdAt = now;
        this.updatedAt = now;
        if (this.type == null) this.type = ProductType.EBOOK;
        if (this.status == null) this.status = ProductStatus.DRAFT;
        if (this.currency == null) this.currency = "XOF";
        if (!this.isMarketplace) this.isMarketplace = true;
        if (this.totalRevenue == null) this.totalRevenue = BigDecimal.ZERO;
        if (this.rating == null) this.rating = BigDecimal.ZERO;
    }

    @PreUpdate
    protected void onUpdate() {
        this.updatedAt = Instant.now();
    }

    public boolean isPublished() {
        return this.status == ProductStatus.PUBLISHED;
    }
}
