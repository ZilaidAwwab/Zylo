package com.example.zylo.product.dto.productDtos;

import lombok.Data;

import java.math.BigDecimal;

@Data
public class ProductSearchRequest {

    private String keyword;
    private Long categoryId;
    private BigDecimal minPrice;
    private BigDecimal maxPrice;
    private Boolean featured;
    private String sortBy = "createdAt"; // name, price, createdAt
    private String sortDir = "desc";     // asc, desc
    private Integer page = 0;
    private Integer size = 20;
}
