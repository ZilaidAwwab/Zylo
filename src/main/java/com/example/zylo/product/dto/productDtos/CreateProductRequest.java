package com.example.zylo.product.dto.productDtos;

import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import lombok.Data;

import java.math.BigDecimal;
import java.util.List;

@Data
public class CreateProductRequest {

    @NotBlank(message = "SKU is required")
    @Size(max = 50, message = "SKU must not exceed 50 characters")
    private String sku;

    @NotBlank(message = "Product name is required")
    @Size(max = 200, message = "Name must not exceed 200 characters")
    private String name;

    @Size(max = 2000, message = "Description must not exceed 2000 characters")
    private String description;

    @NotNull(message = "Price is required")
    @DecimalMin(value = "0.01", message = "Price must be greater than 0")
    private BigDecimal price;

    @NotNull(message = "Stock quantity is required")
    @Min(value = 0, message = "Stock quantity cannot be negative")
    private Integer stockQuantity;

    @Min(value = 1, message = "Low stock threshold must be at least 1")
    private Integer lowStockThreshold = 10;

    private String primaryImageUrl;

    private List<String> additionalImages;

    @NotNull(message = "Category ID is required")
    private Long categoryId;

    private Boolean isActive = true;

    // private Boolean featured = false;

    @Valid
    private ProductSpecsRequest specs;
}
