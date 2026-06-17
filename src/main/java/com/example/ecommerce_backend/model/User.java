package com.example.ecommerce_backend.model;

import jakarta.persistence.*;
import lombok.Data;
import java.time.LocalDateTime;

@Data
@Entity
@Table(name = "users")
public class User {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // 用戶名（登入帳號），必須是唯一的 (unique)
    @Column(nullable = false, unique = true, length = 50)
    private String username;

    // 密碼，因為之後會用 BCrypt 雜湊加密，所以長度留 100 絕對夠長
    @Column(nullable = false, length = 100)
    private String password;

    // 權限角色：儲存 "ADMIN" 或 "USER"
    @Column(nullable = false, length = 20)
    private String role;

    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        this.createdAt = LocalDateTime.now();
    }
}