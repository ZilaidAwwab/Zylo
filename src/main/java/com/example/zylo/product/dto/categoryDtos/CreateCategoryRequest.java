package com.example.zylo.product.dto.categoryDtos;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class CreateCategoryRequest {

    @NotBlank(message = "Category name is required")
    @Size(max = 100, message = "Name must not exceed 100 characters")
    private String name;

    @Size(max = 100, message = "Slug must not exceed 100 characters")
    private String slug;

    @Size(max = 500, message = "Description must not exceed 500 characters")
    private String description;

    private Long parentId; // null for root categories

    private Boolean isActive = true;

    @Min(value = 0, message = "Display order cannot be negative")
    private Integer displayOrder = 0;
}
