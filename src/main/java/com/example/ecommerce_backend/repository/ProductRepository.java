package com.example.ecommerce_backend.repository;

import com.example.ecommerce_backend.model.Product;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;


import java.util.Optional;

public interface ProductRepository extends JpaRepository<Product, Long> {

    // 1. 查詢商品列表：因為要支援「分頁」與「軟刪除」，我們只撈出 isDeleted = false 的商品
    Page<Product> findByIsDeletedFalse(Pageable pageable);

    // 2. 查詢單一商品：同樣要確保該商品沒有被軟刪除
    Optional<Product> findByIdAndIsDeletedFalse(Long id);
}
