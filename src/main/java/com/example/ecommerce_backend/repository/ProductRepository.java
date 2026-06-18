package com.example.ecommerce_backend.repository;

import com.example.ecommerce_backend.model.Product;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface ProductRepository extends JpaRepository<Product, Long> {

    // 1. 查詢商品列表：因為要支援「分頁」與「軟刪除」，我們只撈出 isDeleted = false 的商品
    Page<Product> findByIsDeletedFalse(Pageable pageable);

    // 2. 查詢單一商品：同樣要確保該商品沒有被軟刪除
    Optional<Product> findByIdAndIsDeletedFalse(Long id);

    @Modifying(flushAutomatically = true)
    @Query("""
            update Product p
            set p.stock = p.stock - :quantity,
                p.version = p.version + 1
            where p.id = :productId
              and p.isDeleted = false
              and p.stock >= :quantity
            """)
    int deductStockIfEnough(@Param("productId") Long productId, @Param("quantity") Integer quantity);
}
