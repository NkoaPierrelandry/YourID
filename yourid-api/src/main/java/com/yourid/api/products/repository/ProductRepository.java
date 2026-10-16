package com.yourid.api.products.repository;

import com.yourid.api.products.entity.Product;
import com.yourid.api.products.enums.ProductStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

import java.util.Optional;
import java.util.UUID;

public interface ProductRepository extends JpaRepository<Product, UUID>, JpaSpecificationExecutor<Product> {
    Optional<Product> findBySlug(String slug);
    boolean existsBySlug(String slug);
    long countByStoreId(UUID storeId);
    Page<Product> findByStoreId(UUID storeId, Pageable pageable);

    // Marketplace publique : ne montre que les produits publies et marques visibles sur le marketplace.
    Page<Product> findByStatusAndIsMarketplaceTrue(ProductStatus status, Pageable pageable);
    Page<Product> findByStatusAndIsFeaturedTrueAndIsMarketplaceTrue(ProductStatus status, Pageable pageable);
    Page<Product> findByStatusAndIsMarketplaceTrueAndCategoryId(ProductStatus status, UUID categoryId, Pageable pageable);
}
