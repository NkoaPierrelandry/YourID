package com.yourid.api.products.controller;

import com.yourid.api.common.response.ApiResponse;
import com.yourid.api.common.security.UserPrincipal;
import com.yourid.api.products.dto.CreateProductRequest;
import com.yourid.api.products.dto.ProductResponse;
import com.yourid.api.products.dto.UpdateProductRequest;
import com.yourid.api.products.service.ProductService;
import com.yourid.api.products.service.ProductService.ContentDownload;
import org.springframework.core.io.InputStreamResource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.web.multipart.MultipartFile;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/products")
@RequiredArgsConstructor
public class ProductController {

    private final ProductService productService;

    @PostMapping
    public ResponseEntity<ApiResponse<ProductResponse>> create(
            @AuthenticationPrincipal UserPrincipal principal,
            @Valid @RequestBody CreateProductRequest request
    ) {
        ProductResponse response = productService.create(principal.getId(), request);
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.ok(response, "Produit cree."));
    }

    @PatchMapping("/{id}")
    public ApiResponse<ProductResponse> update(
            @AuthenticationPrincipal UserPrincipal principal,
            @PathVariable UUID id,
            @Valid @RequestBody UpdateProductRequest request
    ) {
        return ApiResponse.ok(productService.update(principal.getId(), id, request), "Produit mis a jour.");
    }

    @DeleteMapping("/{id}")
    public ApiResponse<Void> delete(@AuthenticationPrincipal UserPrincipal principal, @PathVariable UUID id) {
        productService.delete(principal.getId(), id);
        return ApiResponse.ok(null, "Produit supprime.");
    }

    @GetMapping("/me")
    public ApiResponse<Page<ProductResponse>> listMine(
            @AuthenticationPrincipal UserPrincipal principal,
            Pageable pageable
    ) {
        return ApiResponse.ok(productService.listMine(principal.getId(), pageable));
    }

    // Route publique : fiche produit, ex: yourid.com/p/mon-ebook-slug
    @GetMapping("/{slug}")
    public ApiResponse<ProductResponse> getBySlug(@PathVariable String slug) {
        return ApiResponse.ok(productService.getBySlugPublic(slug));
    }

    @PostMapping("/{id}/content")
    public ApiResponse<ProductResponse> uploadContent(
            @AuthenticationPrincipal UserPrincipal principal,
            @PathVariable UUID id,
            @RequestParam("file") MultipartFile file
    ) {
        return ApiResponse.ok(productService.uploadContent(principal.getId(), id, file), "Fichier de contenu televerse.");
    }

    // Permet au vendeur de re-telecharger son propre fichier pour verifier ce qu il a envoye.
    @GetMapping("/{id}/content/download")
    public ResponseEntity<InputStreamResource> downloadOwnContent(
            @AuthenticationPrincipal UserPrincipal principal,
            @PathVariable UUID id
    ) {
        ContentDownload dl = productService.downloadForOwner(principal.getId(), id);
        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType(dl.contentType()))
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + dl.fileName() + "\"")
                .body(new InputStreamResource(dl.stream()));
    }
}
