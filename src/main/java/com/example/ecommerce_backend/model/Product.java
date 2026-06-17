package com.example.ecommerce_backend.model;

import jakarta.persistence.*;
import lombok.Data;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data // Lombok 註解：自動幫你生成所有欄位的 Getter/Setter/toString
@Entity // 告訴 Spring Boot 這是一個資料庫實體類別
@Table(name = "products") // 指定在 PostgreSQL 中的資料表名稱為 products
public class Product {

    @Id // 標記為主鍵 (Primary Key)
    @GeneratedValue(strategy = GenerationType.IDENTITY) // 設定為自動遞增 (Serial)
    private Long id;

    @Column(nullable = false, length = 100) // 設定為不允許為空值，最大長度 100
    private String name;

    
    @Column(nullable = false, precision = 10, scale = 2) 
    private BigDecimal price;

    @Column(nullable = false)
    private Integer stock;

    // 實作軟刪除（Soft Delete）的關鍵核心：預設為 false (未刪除)
    @Column(name = "is_deleted", nullable = false)
    private Boolean isDeleted = false;

    @Column(name = "created_at", updatable = false) // 設定建立後就不能被修改
    private LocalDateTime createdAt;

    // 當這筆資料第一次被存入資料庫時，自動帶入當前伺服器時間
    @PrePersist
    protected void onCreate() {
        this.createdAt = LocalDateTime.now();
    }
}