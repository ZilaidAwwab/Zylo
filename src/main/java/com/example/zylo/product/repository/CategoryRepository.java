package com.example.zylo.product.repository;

import com.example.zylo.product.entity.Category;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface CategoryRepository extends JpaRepository<Category, Long> {

    Optional<Category> findBySlug(String slug);
    boolean existsBySlug(String slug);
    List<Category> findByIsActiveTrueOrderByDisplayOrderAsc();
    List<Category> findByParentIsNullAndIsActiveTrueOrderByDisplayOrderAsc();
    List<Category> findByParentIdAndIsActiveTrueOrderByDisplayOrderAsc(Long parentId);

    // @Query("SELECT c FROM Category c LEFT JOIN FETCH c.children WHERE c.parent IS NULL AND c.isActive = true ORDER BY c.displayOrder")
    // List<Category> findRootCategoriesWithChildren();

    @Query("SELECT c FROM Category c LEFT JOIN FETCH c.parent ORDER BY c.displayOrder")
    List<Category> findAllWithParent();

    long countByParentId(Long parentId);
}
