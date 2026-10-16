package com.yourid.api.products.service;

import com.yourid.api.categories.entity.Category;
import com.yourid.api.categories.repository.CategoryRepository;
import com.yourid.api.common.exception.BadRequestException;
import com.yourid.api.common.exception.ResourceNotFoundException;
import com.yourid.api.common.util.SlugUtil;
import com.yourid.api.products.dto.CreateProductRequest;
import com.yourid.api.products.dto.ProductResponse;
import com.yourid.api.products.dto.UpdateProductRequest;
import com.yourid.api.products.entity.Product;
import com.yourid.api.products.enums.ProductStatus;
import com.yourid.api.products.repository.ProductRepository;
import com.yourid.api.stores.entity.Store;
import com.yourid.api.stores.service.StoreService;
import com.yourid.api.files.service.ContentFileStorageService;
import com.yourid.api.products.enums.ContentType;
import org.springframework.web.multipart.MultipartFile;
import java.io.InputStream;
import java.net.URLConnection;

import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
@RequiredArgsConstructor
public class ProductService {

    private final ProductRepository productRepository;
    private final CategoryRepository categoryRepository;
    private final StoreService storeService;
    private final ContentFileStorageService contentFileStorageService; // nouveau champ
    
    @Transactional
    public ProductResponse create(UUID userId, CreateProductRequest request) {
        Store store = storeService.findByUserId(userId);
        assertProductLimit(store);

        Category category = null;
        if (request.getCategoryId() != null) {
            category = categoryRepository.findById(request.getCategoryId())
                    .orElseThrow(() -> new ResourceNotFoundException("Categorie introuvable."));
        }

        String slug = generateUniqueSlug(request.getTitle());

        Product product = Product.builder()
                .store(store)
                .category(category)
                .type(request.getType())
                .title(request.getTitle())
                .slug(slug)
                .description(request.getDescription())
                .shortDesc(request.getShortDesc())
                .price(request.getPrice())
                .comparePrice(request.getComparePrice())
                .currency(request.getCurrency())
                .contentUrl(request.getContentUrl())
                .contentType(request.getContentType())
                .contentNote(request.getContentNote())
                .build();

        return toResponse(productRepository.save(product));
    }

    @Transactional
    public ProductResponse update(UUID userId, UUID productId, UpdateProductRequest request) {
        Product product = findEntity(productId);
        storeService.assertOwnership(product.getStore(), userId);

        if (request.getTitle() != null) product.setTitle(request.getTitle());
        if (request.getDescription() != null) product.setDescription(request.getDescription());
        if (request.getShortDesc() != null) product.setShortDesc(request.getShortDesc());
        if (request.getType() != null) product.setType(request.getType());
        if (request.getStatus() != null) product.setStatus(request.getStatus());
        if (request.getPrice() != null) product.setPrice(request.getPrice());
        if (request.getComparePrice() != null) product.setComparePrice(request.getComparePrice());
        if (request.getCoverImage() != null) product.setCoverImage(request.getCoverImage());
        if (request.getContentUrl() != null) product.setContentUrl(request.getContentUrl());
        if (request.getContentType() != null) product.setContentType(request.getContentType());
        if (request.getContentNote() != null) product.setContentNote(request.getContentNote());

        if (request.getCategoryId() != null) {
            Category category = categoryRepository.findById(request.getCategoryId())
                    .orElseThrow(() -> new ResourceNotFoundException("Categorie introuvable."));
            product.setCategory(category);
        }

        return toResponse(productRepository.save(product));
    }

    @Transactional
    public void delete(UUID userId, UUID productId) {
        Product product = findEntity(productId);
        storeService.assertOwnership(product.getStore(), userId);
        productRepository.delete(product);
    }

    // Consultation publique : n expose que les produits PUBLISHED, incremente le compteur de vues.
    @Transactional
    public ProductResponse getBySlugPublic(String slug) {
        Product product = productRepository.findBySlug(slug)
                .orElseThrow(() -> new ResourceNotFoundException("Produit introuvable : " + slug));
        if (product.getStatus() != ProductStatus.PUBLISHED) {
            throw new ResourceNotFoundException("Produit introuvable : " + slug);
        }
        product.setViewCount(product.getViewCount() + 1);
        return toResponse(productRepository.save(product));
    }

    // Utilisees par MarketplaceController -- lecture publique, aucune authentification.
    @Transactional(readOnly = true)
    public Page<ProductResponse> listMarketplace(Pageable pageable) {
        return productRepository.findByStatusAndIsMarketplaceTrue(ProductStatus.PUBLISHED, pageable).map(this::toResponse);
    }

    @Transactional(readOnly = true)
    public Page<ProductResponse> listFeatured(Pageable pageable) {
        return productRepository.findByStatusAndIsFeaturedTrueAndIsMarketplaceTrue(ProductStatus.PUBLISHED, pageable).map(this::toResponse);
    }

    @Transactional(readOnly = true)
    public Page<ProductResponse> listByCategory(UUID categoryId, Pageable pageable) {
        return productRepository.findByStatusAndIsMarketplaceTrueAndCategoryId(ProductStatus.PUBLISHED, categoryId, pageable).map(this::toResponse);
    }

    @Transactional(readOnly = true)
    public Page<ProductResponse> listMine(UUID userId, Pageable pageable) {
        Store store = storeService.findByUserId(userId);
        return productRepository.findByStoreId(store.getId(), pageable).map(this::toResponse);
    }

    // Utilise en interne par OrderService pour recuperer le produit complet
    // (avec son contentUrl) lors de la creation d une commande.
    public Product findEntity(UUID productId) {
        return productRepository.findById(productId)
                .orElseThrow(() -> new ResourceNotFoundException("Produit introuvable."));
    }

    private void assertProductLimit(Store store) {
        int maxProducts = store.getPlan().getMaxProducts();
        if (store.getPlan().isUnlimited()) {
            return;
        }

        long currentProducts = productRepository.countByStoreId(store.getId());
        if (currentProducts >= maxProducts) {
            throw new BadRequestException(
                    "Limite de produits atteinte pour votre plan " + store.getPlan() + ". " +
                    "Le plan actuel autorise jusqu'à " + maxProducts + " produits."
            );
        }
    }

    private String generateUniqueSlug(String title) {
        String base = SlugUtil.slugify(title);
        String slug = base;
        int suffix = 1;
        while (productRepository.existsBySlug(slug)) {
            slug = base + "-" + suffix++;
        }
        return slug;
    }

        private ProductResponse toResponse(Product product) {
        return ProductResponse.builder()
                .id(product.getId())
                .storeId(product.getStore().getId())
                .storeName(product.getStore().getName())
                .categoryId(product.getCategory() != null ? product.getCategory().getId() : null)
                .categoryName(product.getCategory() != null ? product.getCategory().getName() : null)
                .type(product.getType())
                .status(product.getStatus())
                .title(product.getTitle())
                .slug(product.getSlug())
                .description(product.getDescription())
                .shortDesc(product.getShortDesc())
                .coverImage(product.getCoverImage())
                .price(product.getPrice())
                .comparePrice(product.getComparePrice())
                .currency(product.getCurrency())
                .contentUrl(product.getContentUrl())
                .contentType(product.getContentType())
                .rating(product.getRating())
                .reviewCount(product.getReviewCount())
                .totalSales(product.getTotalSales())
                .viewCount(product.getViewCount())
                .createdAt(product.getCreatedAt())
                .contentFileName(product.getContentFileName())
                .build();
    }

        public record ContentDownload(InputStream stream, String fileName, String contentType) {}

    @Transactional
    public ProductResponse uploadContent(UUID userId, UUID productId, MultipartFile file) {
        Product product = findEntity(productId);
        storeService.assertOwnership(product.getStore(), userId);

        ContentFileStorageService.StoredFile stored = contentFileStorageService.store(file);
        product.setContentFilePath(stored.storedName());
        product.setContentFileName(stored.originalName());
        product.setContentType(ContentType.FILE);
        product.setContentUrl(null); // un produit a soit un lien externe, soit un fichier heberge, jamais les deux

        return toResponse(productRepository.save(product));
    }

    // Ne verifie AUCUN droit d acces -- c est aux appelants (voir downloadForOwner
    // ci-dessous, et OrderService.downloadForBuyer) de l avoir fait avant d appeler.
    public ContentDownload openContent(Product product) {
        // if (product.getContentType() != ContentType.FILE || product.getContentFilePath() == null) {
        //     throw new BadRequestEexception("Ce produit n a pas de fichier heberge (lien externe uniquement).");
        // }
        InputStream stream = contentFileStorageService.read(product.getContentFilePath());
        String mime = URLConnection.guessContentTypeFromName(product.getContentFileName());
        return new ContentDownload(stream, product.getContentFileName(), mime != null ? mime : "application/octet-stream");
    }

    @Transactional(readOnly = true)
    public ContentDownload downloadForOwner(UUID userId, UUID productId) {
        Product product = findEntity(productId);
        storeService.assertOwnership(product.getStore(), userId);
        return openContent(product);
    }
}
