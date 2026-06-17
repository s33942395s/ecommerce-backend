package com.example.ecommerce_backend.repository;
import com.example.ecommerce_backend.model.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface UserRepository extends JpaRepository<User, Long> {
    
    // 透過帳號（username）撈出用戶資料來比對密碼
    Optional<User> findByUsername(String username);
    
    // 註冊時檢查：確認這個帳號有沒有人註冊過了
    boolean existsByUsername(String username);
}
