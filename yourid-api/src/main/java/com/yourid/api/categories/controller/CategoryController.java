package com.yourid.api.categories.controller;

import com.yourid.api.categories.entity.Category;
import com.yourid.api.categories.repository.CategoryRepository;
import com.yourid.api.common.response.ApiResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/v1/categories")
@RequiredArgsConstructor
public class CategoryController {

    private final CategoryRepository categoryRepository;

    @GetMapping
    public ApiResponse<List<Category>> list() {
        return ApiResponse.ok(categoryRepository.findAllByOrderBySortOrderAsc());
    }
}
