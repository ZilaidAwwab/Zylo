package com.example.zylo.product.service.impl;

import com.example.zylo.common.dto.ApiResponse;
import com.example.zylo.common.exception.ResourceNotFoundException;
import com.example.zylo.product.dto.productDtos.*;
import com.example.zylo.product.entity.Category;
import com.example.zylo.product.entity.Product;
import com.example.zylo.product.entity.ProductSpecs;
import com.example.zylo.product.repository.CategoryRepository;
import com.example.zylo.product.repository.ProductRepository;
import com.example.zylo.product.repository.ProductSpecsRepository;
import com.example.zylo.product.service.ProductService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class ProductServiceImpl implements ProductService {

    private final ProductRepository productRepository;
    private final ProductSpecsRepository productSpecsRepository;
    private final CategoryRepository categoryRepository;

    // Create Product
    @Override
    @Transactional
    @CacheEvict(value = "products", allEntries = true)
    public ProductResponse createProduct(CreateProductRequest request) {

        // Validate SKU uniqueness
        if (productRepository.existsBySku(request.getSku())) {
            throw new IllegalArgumentException("SKU already exists: " + request.getSku());
        }

        // Validate category exists
        Category category = categoryRepository.findById(request.getCategoryId())
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Category not found with id: " + request.getCategoryId()));

        // Build product
        Product product = Product.builder()
                .sku(request.getSku())
                .name(request.getName())
                .description(request.getDescription())
                .basePrice(request.getPrice())
                .stockQuantity(request.getStockQuantity())
                .lowStockThreshold(request.getLowStockThreshold())
                .primaryImageUrl(request.getPrimaryImageUrl())
                .additionalImages(request.getAdditionalImages())
                .isActive(request.getIsActive())
                .category(category)
                .build();

        product = productRepository.save(product);

        // Create specs if provided
        if (request.getSpecs() != null) {
            ProductSpecs productSpecs = buildSpecs(request.getSpecs(), product);
            product.setSpecs(productSpecsRepository.save(productSpecs));
        }

        log.info("Product created: {} (SKU: {})", product.getName(), product.getSku());
        return mapToResponse(product);
    }

    // Update Product
    @Transactional
    @CacheEvict(value = {"products", "product"}, allEntries = true)
    public ProductResponse updateProduct(Long id, UpdateProductRequest request) {

        Product product = productRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Product not found with id: " + id));

        // Update fields if provided
        if (request.getName() != null) product.setName(request.getName());
        if (request.getDescription() != null) product.setDescription(request.getDescription());
        if (request.getPrice() != null) product.setBasePrice(request.getPrice());
        if (request.getStockQuantity() != null) product.setStockQuantity(request.getStockQuantity());
        if (request.getLowStockThreshold() != null) product.setLowStockThreshold(request.getLowStockThreshold());
        if (request.getPrimaryImageUrl() != null) product.setPrimaryImageUrl(request.getPrimaryImageUrl());
        if (request.getAdditionalImages() != null) product.setAdditionalImages(request.getAdditionalImages());
        if (request.getIsActive() != null) product.setIsActive(request.getIsActive());

        // Update category if provided
        if (request.getCategoryId() != null) {
            Category category = categoryRepository.findById(request.getCategoryId())
                    .orElseThrow(() -> new ResourceNotFoundException(
                            "Category not found with id: " + request.getCategoryId()));
            product.setCategory(category);
        }

        // Update specs if provided
        if (request.getSpecs() != null) {
            ProductSpecs specs = product.getSpecs();
            if (specs == null) {
                specs = buildSpecs(request.getSpecs(), product);
                product.setSpecs(productSpecsRepository.save(specs));
            } else {
                updateSpecs(specs, request.getSpecs());
                productSpecsRepository.save(specs);
            }
        }

        product = productRepository.save(product);
        log.info("Product updated: {}", product.getSku());
        return mapToResponse(product);
    }

    // Get Product by Id
    @Cacheable(value = "product", key = "#id")
    public ProductResponse getProductById(Long id) {

        Product product = productRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Product not found with id: " + id));
        return mapToResponse(product);
    }

    // Get all products (paginated)
    @Cacheable(value = "products", key = "#pageable.pageNumber + '-' + #pageable.pageSize")
    public Page<ProductResponse> getAllProducts(Pageable pageable) {

        Page<Product> products = productRepository.findByIsActiveTrue(pageable);
        return products.map(this::mapToResponse);
    }

    // Search and filter products
    public Page<ProductResponse> searchProducts(ProductSearchRequest request) {

        Pageable pageable = PageRequest.of(
                request.getPage(),
                request.getSize(),
                Sort.by(
                        request.getSortDir().equalsIgnoreCase("asc")
                        ? Sort.Direction.ASC : Sort.Direction.DESC,
                        request.getSortBy()
                )
        );

        Specification<Product> spec = (root, query, cb) -> cb.isTrue(root.get("isActive"));

        // Keyword search
        if (request.getKeyword() != null && !request.getKeyword().isEmpty()) {
            String keyword = "%" + request.getKeyword().toLowerCase() + "%";
            spec = spec.and((root, query, cb) ->
                    cb.like(cb.lower(root.get("name")), keyword));
        }

        // Category filter
        if (request.getCategoryId() != null) {
            spec = spec.and((root, query, cb) ->
                    cb.equal(root.get("category").get("id"), request.getCategoryId()));
        }

        // Price range filter
        if (request.getMinPrice() != null) {
            spec = spec.and((root, query, cb) ->
                    cb.greaterThanOrEqualTo(root.get("price"), request.getMinPrice()));
        }

        if (request.getMaxPrice() != null) {
            spec = spec.and((root, query, cb) ->
                    cb.lessThanOrEqualTo(root.get("price"), request.getMaxPrice()));
        }

        Page<Product> products = productRepository.findAll(spec, pageable);
        return products.map(this::mapToResponse);
    }

    // Get products by category
    public Page<ProductResponse> getProductsByCategory(Long categoryId, Pageable pageable) {

        Page<Product> products = productRepository.findByCategoryIdAndIsActiveTrue(categoryId, pageable);
        return products.map(this::mapToResponse);
    }

    // Get featured Products
    // public List<ProductResponse> getFeaturedProducts() {}

    // Delete products (soft delete)
    @Transactional
    @CacheEvict(value = {"products", "product"}, allEntries = true)
    public ApiResponse<String> deleteProduct(Long id) {

        Product product = productRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Product not found with id: " + id));

        // soft delete
        product.setIsActive(false);
        productRepository.save(product);

        log.info("Product soft deleted: {}", product.getSku());
        return ApiResponse.success("Product deleted successfully");
    }

    // Stock management
    @Transactional
    public void updateStock(Long productId, int quantity) {

        Product product = productRepository.findById(productId)
                .orElseThrow(() -> new ResourceNotFoundException("Product not found with id: " + productId));

        product.setStockQuantity(quantity);
        productRepository.save(product);
        log.info("Stock updated for {}: {}", product.getSku(), quantity);
    }

    @Transactional
    public void decrementStock(Long productId, int quantity) {

        Product product = productRepository.findById(productId)
                .orElseThrow(() -> new ResourceNotFoundException("Product not found with id: " + productId));

        product.decrementStock(quantity);
        productRepository.save(product);
        log.info("Stock decremented for {}: -{}", product.getSku(), quantity);
    }

    public List<ProductResponse> getLowStockProducts() {
        return productRepository.findLowStockProducts()
                .stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    // Helper methods
    private ProductSpecs buildSpecs(ProductSpecsRequest request, Product product) {
        return ProductSpecs.builder()
                .product(product)
                .processor(request.getProcessor())
                .ram(request.getRam())
                .storage(request.getStorage())
                .screenSize(request.getScreenSize())
                .screenType(request.getScreenType())
                .color(request.getColor())
                .batteryCapacity(request.getBatteryCapacity())
                .cameraSystem(request.getCamera())
                .connectivity(request.getConnectivity())
                .ports(request.getPorts())
                .features(request.getAdditionalFeatures())
                .build();
    }

    private void updateSpecs(ProductSpecs specs, ProductSpecsRequest request) {
        if (request.getProcessor() != null) specs.setProcessor(request.getProcessor());
        if (request.getRam() != null) specs.setRam(request.getRam());
        if (request.getStorage() != null) specs.setStorage(request.getStorage());
        if (request.getScreenSize() != null) specs.setScreenSize(request.getScreenSize());
        if (request.getScreenType() != null) specs.setScreenType(request.getScreenType());
        if (request.getColor() != null) specs.setColor(request.getColor());
        if (request.getBatteryCapacity() != null) specs.setBatteryCapacity(request.getBatteryCapacity());
        if (request.getCamera() != null) specs.setCameraSystem(request.getCamera());
        if (request.getConnectivity() != null) specs.setConnectivity(request.getConnectivity());
        if (request.getPorts() != null) specs.setPorts(request.getPorts());
        if (request.getAdditionalFeatures() != null) specs.setFeatures(request.getAdditionalFeatures());
    }

    private ProductResponse mapToResponse(Product product) {
        ProductResponse.CategorySummary categorySummary = null;
        if (product.getCategory() != null) {
            categorySummary = ProductResponse.CategorySummary.builder()
                    .id(product.getCategory().getId())
                    .name(product.getCategory().getName())
                    .slug(product.getCategory().getSlug())
                    .build();
        }

        ProductSpecsResponse specsResponse = null;
        if (product.getSpecs() != null) {
            ProductSpecs specs = product.getSpecs();
            specsResponse = ProductSpecsResponse.builder()
                    .processor(specs.getProcessor())
                    .ram(specs.getRam())
                    .storage(specs.getStorage())
                    .screenSize(specs.getScreenSize())
                    .screenType(specs.getScreenType())
                    .color(specs.getColor())
                    .batteryCapacity(specs.getBatteryCapacity())
                    .camera(specs.getCameraSystem())
                    .connectivity(specs.getConnectivity())
                    .ports(specs.getPorts())
                    .additionalFeatures(specs.getFeatures())
                    .build();
        }

        return ProductResponse.builder()
                .id(product.getId())
                .sku(product.getSku())
                .name(product.getName())
                .description(product.getDescription())
                .price(product.getBasePrice())
                .stockQuantity(product.getStockQuantity())
                .stockStatus(product.getStockStatus())
                .primaryImageUrl(product.getPrimaryImageUrl())
                .additionalImages(product.getAdditionalImages())
                .isActive(product.getIsActive())
                .category(categorySummary)
                .specs(specsResponse)
                .createdAt(product.getCreatedAt())
                .updatedAt(product.getUpdatedAt())
                .build();
    }
}
