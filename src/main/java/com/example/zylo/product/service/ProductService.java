package com.example.zylo.product.service;

import com.example.zylo.common.dto.ApiResponse;
import com.example.zylo.product.dto.productDtos.CreateProductRequest;
import com.example.zylo.product.dto.productDtos.ProductResponse;
import com.example.zylo.product.dto.productDtos.ProductSearchRequest;
import com.example.zylo.product.dto.productDtos.UpdateProductRequest;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.List;

public interface ProductService {

    ProductResponse createProduct(CreateProductRequest request);
    ProductResponse updateProduct(Long id, UpdateProductRequest request);
    ProductResponse getProductById(Long id);
    Page<ProductResponse> getAllProducts(Pageable pageable);
    Page<ProductResponse> searchProducts(ProductSearchRequest request);
    Page<ProductResponse> getProductsByCategory(Long categoryId, Pageable pageable);
    ApiResponse<String> deleteProduct(Long id);
    void updateStock(Long productId, int quantity);
    void decrementStock(Long productId, int quantity);
    List<ProductResponse> getLowStockProducts();
}
