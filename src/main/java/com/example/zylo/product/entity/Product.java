package com.example.zylo.product.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Entity
@Table(name = "product", indexes = {
        @Index(name = "idx_category", columnList = "category_id"),
        @Index(name = "idx_sku", columnList = "sku"),
        @Index(name = "idx_active", columnList = "is_active"),
        @Index(name = "idx_stock", columnList = "stock_status"),
        @Index(name = "idx_rating", columnList = "average_rating"),
        @Index(name = "idx_price", columnList = "base_price")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Product {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(unique = true, nullable = false, length = 50)
    private String sku;

    @Column(nullable = false)
    private String name;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "category_id")
    private Category category;

    @Column(columnDefinition = "TEXT")
    private String description;

    @Column(name = "base_price", nullable = false, precision = 10, scale = 2)
    private BigDecimal basePrice;

    @Column(name = "stock_quantity")
    private Integer stockQuantity = 0;

    @Column(name = "stock_status", insertable = false, updatable = false)
    @Enumerated(EnumType.STRING)
    private StockStatus stockStatus;

    @Column(name = "low_stock_threshold")
    private Integer lowStockThreshold = 10;

    @Column(name = "items_sold")
    private Integer itemsSold = 0;

    @Column(name = "average_rating", precision = 3, scale = 2)
    private BigDecimal averageRating = BigDecimal.ZERO;

    @Column(name = "total_reviews")
    private Integer totalReviews = 0;

    @Column(name = "primary_image_url", length = 500)
    private String primaryImageUrl;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "additional_images", columnDefinition = "json")
    private List<String> additionalImages;

    @Column(name = "is_active")
    private Boolean isActive = true;

    @OneToOne(mappedBy = "product", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    private ProductSpecs specs;

    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }

    public enum StockStatus {
        IN_STOCK, OUT_OF_STOCK, LOW_STOCK
    }
}
