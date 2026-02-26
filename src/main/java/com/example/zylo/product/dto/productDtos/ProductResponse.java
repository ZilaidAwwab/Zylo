package com.example.zylo.product.dto.productDtos;

import com.example.zylo.product.entity.Product;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ProductResponse {

    private Long id;
    private String sku;
    private String name;
    private String description;
    private BigDecimal price;
    private Integer stockQuantity;
    private Product.StockStatus stockStatus;
    private String primaryImageUrl;
    private List<String> additionalImages;
    private Boolean isActive;
    // private Boolean featured;
    private CategorySummary category;
    private ProductSpecsResponse specs;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class CategorySummary {
        private Long id;
        private String name;
        private String slug;
    }
}
