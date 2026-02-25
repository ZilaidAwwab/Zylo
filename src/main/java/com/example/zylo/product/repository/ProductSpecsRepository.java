package com.example.zylo.product.repository;

import com.example.zylo.product.entity.ProductSpecs;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface ProductSpecsRepository extends JpaRepository<ProductSpecs, Long> {

    Optional<ProductSpecs> findByProductId(Long productId);
    void deleteByProductId(Long productId);
}
