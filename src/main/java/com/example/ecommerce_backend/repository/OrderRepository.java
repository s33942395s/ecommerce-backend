package com.example.ecommerce_backend.repository;

import com.example.ecommerce_backend.model.Order;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;


public interface OrderRepository extends JpaRepository<Order, Long> {
    // 未來可以擴充：透過用戶 ID 查詢該用戶的所有歷史訂單
}