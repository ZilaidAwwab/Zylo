package com.example.zylo.product.dto.productDtos;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ProductSpecsResponse {

    private String processor;
    private String ram;
    private String storage;
    private String screenSize;
    private String screenType;
    private String color;
    private String batteryCapacity;
    private String camera;
    private List<String> connectivity;
    private List<String> ports;
    private List<String> additionalFeatures;
}
