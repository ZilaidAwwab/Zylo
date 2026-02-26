package com.example.zylo.product.dto.productDtos;

import jakarta.validation.Valid;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.math.BigDecimal;
import java.util.List;

@Data
public class UpdateProductRequest {

    @Size(max = 200, message = "Name must not exceed 200 characters")
    private String name;

    @Size(max = 2000, message = "Description must not exceed 2000 characters")
    private String description;

    @DecimalMin(value = "0.01", message = "Price must be greater than 0")
    private BigDecimal price;

    @Min(value = 0, message = "Stock quantity cannot be negative")
    private Integer stockQuantity;

    @Min(value = 1, message = "Low threshold stock must be at least 1")
    private Integer lowStockThreshold;

    private String primaryImageUrl;

    private List<String> additionalImages;

    private Long categoryId;

    private Boolean isActive;

    private Boolean featured;

    @Valid
    private ProductSpecsRequest specs;
}
