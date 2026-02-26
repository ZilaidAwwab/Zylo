package com.example.zylo.product.dto.categoryDtos;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CategoryResponse {

    private Long id;
    private String name;
    private String slug;
    private String description;
    private Boolean isActive;
    private Integer displayOrder;
    private Long parentId;
    private String parentName;
    private List<CategoryResponse> childern;
    // private Long productCount;
    private LocalDateTime createdAt;
}
