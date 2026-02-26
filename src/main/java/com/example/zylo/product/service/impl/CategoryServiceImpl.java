package com.example.zylo.product.service.impl;

import com.example.zylo.common.dto.ApiResponse;
import com.example.zylo.common.exception.ResourceNotFoundException;
import com.example.zylo.product.dto.categoryDtos.CategoryResponse;
import com.example.zylo.product.dto.categoryDtos.CreateCategoryRequest;
import com.example.zylo.product.entity.Category;
import com.example.zylo.product.repository.CategoryRepository;
import com.example.zylo.product.service.CategoryService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class CategoryServiceImpl implements CategoryService {

    private final CategoryRepository categoryRepository;

    // Create Category
    @Transactional
    @CacheEvict(value = "categories", allEntries = true)
    public CategoryResponse createCategory(CreateCategoryRequest request) {

        // Validates slug uniqueness
        if (request.getSlug() != null && categoryRepository.existsBySlug(request.getSlug())) {
            throw new IllegalArgumentException("Slug already exists: " + request.getSlug());
        }

        Category parent = null;
        if (request.getParentId() != null) {
            parent = categoryRepository.findById(request.getParentId())
                    .orElseThrow(() -> new ResourceNotFoundException(
                            "Parent category not found with id: " + request.getParentId()
                    ));
        }

        Category category = Category.builder()
                .name(request.getName())
                .slug(request.getSlug())
                .description(request.getDescription())
                .parent(parent)
                .isActive(request.getIsActive())
                .displayOrder(request.getDisplayOrder())
                .build();

        category = categoryRepository.save(category);
        log.info("Category created: {} (slug: {})", category.getName(), category.getSlug());

        return mapToResponse(category);
    }

    // Get all categories
    @Cacheable(value = "categories", key = "'all'")
    public List<CategoryResponse> getAllCategories() {
         List<Category> rootCategories = categoryRepository.findAllWithParent();
         return rootCategories.stream()
                 .map(this::mapToResponse)
                 .collect(Collectors.toList());
    }

    // Get root categories
    public List<CategoryResponse> getRootCategories() {
        List<Category> categories = categoryRepository.findByIsActiveTrueOrderByDisplayOrderAsc();
        return categories.stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    // Get category by id
    public CategoryResponse getCategoryById(Long id) {
        Category category = categoryRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Category not found with id: " + id));
        return mapToResponse(category);
    }

    // Get category by slug
    public CategoryResponse getCategoryBySlug(String slug) {
        Category category = categoryRepository.findBySlug(slug)
                .orElseThrow(() -> new ResourceNotFoundException("Category not found with slug: " + slug));
        return mapToResponse(category);
    }

    // Delete category
    @Transactional
    @CacheEvict(value = "categories", allEntries = true)
    public ApiResponse<String> deleteCategory(Long id) {
        Category category = categoryRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Category not found with id: " + id));

        // Check if it has children (Not implemented)

        // Check if category has products (No relation in the category table)

        categoryRepository.delete(category);
        log.info("Category deleted: {}", category.getName());
        return ApiResponse.success("Category deleted successfully");
    }

    // Helper methods
    private CategoryResponse mapToResponse(Category category) {
        return CategoryResponse.builder()
                .id(category.getId())
                .name(category.getName())
                .slug(category.getSlug())
                .description(category.getDescription())
                .isActive(category.getIsActive())
                .displayOrder(category.getDisplayOrder())
                .parentId(category.getParent() != null ? category.getParent().getId() : null)
                .parentName(category.getParent() != null ? category.getParent().getName() : null)
                .createdAt(category.getCreatedAt())
                .build();
    }
}
