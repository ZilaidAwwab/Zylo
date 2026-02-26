package com.example.zylo.product.service;

import com.example.zylo.common.dto.ApiResponse;
import com.example.zylo.product.dto.categoryDtos.CategoryResponse;
import com.example.zylo.product.dto.categoryDtos.CreateCategoryRequest;

import java.util.List;

public interface CategoryService {

    CategoryResponse createCategory(CreateCategoryRequest request);
    List<CategoryResponse> getAllCategories();
    List<CategoryResponse> getRootCategories();
    CategoryResponse getCategoryById(Long id);
    CategoryResponse getCategoryBySlug(String slug);
    ApiResponse<String> deleteCategory(Long id);
}
