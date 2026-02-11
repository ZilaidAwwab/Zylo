package com.example.zylo.product.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.util.List;

@Entity
@Table(name = "product_specs", indexes = {
        @Index(name = "idx_product_id", columnList = "product_id"),
        @Index(name = "idx_ram", columnList = "ram"),
        @Index(name = "idx_storage", columnList = "storage"),
        @Index(name = "idx_color", columnList = "color")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ProductSpecs {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne
    @JoinColumn(name = "product_id", unique = true, nullable = false)
    private Product product;

    @Column(length = 100)
    private String brand = "Apple";

    @Column(name = "model_year")
    private Integer modelYear;

    @Column(name = "warranty_months")
    private Integer warrantyMonths = 12;

    @Column(length = 100)
    private String processor;

    @Column(length = 20)
    private String ram;

    @Column(length = 20)
    private String storage;

    @Column(name = "screen_size", length = 20)
    private String screenSize;

    @Column(name = "screen_resolution", length = 50)
    private String screenResolution;

    @Column(name = "screen_type", length = 50)
    private String screenType;

    @Column(length = 50)
    private String color;

    @Column(name = "weight_grams")
    private Integer weightGrams;

    @Column(length = 100)
    private String dimensions;

    @Column(name = "camera_system", length = 200)
    private String cameraSystem;

    @Column(name = "front_camera", length = 100)
    private String frontCamera;

    @Column(name = "battery_capacity", length = 50)
    private String batteryCapacity;

    @Column(name = "battery_life", length = 100)
    private String batteryLife;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(columnDefinition = "json")
    private List<String> connectivity;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(columnDefinition = "json")
    private List<String> ports;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(columnDefinition = "json")
    private List<String> features;

    @Column(name = "is_active")
    private Boolean isActive = true;
}
