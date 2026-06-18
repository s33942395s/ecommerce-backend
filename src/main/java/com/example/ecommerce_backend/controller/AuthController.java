package com.example.ecommerce_backend.controller;

import com.example.ecommerce_backend.model.User;
import com.example.ecommerce_backend.repository.UserRepository;
import com.example.ecommerce_backend.util.JwtUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private JwtUtil jwtUtil;

    // 1. 用戶註冊 API
    // POST http://localhost:8080/api/auth/register
    @PostMapping("/register")
    public ResponseEntity<?> register(@RequestBody User user) {
        //空帳密檢查
        if (user == null || isBlank(user.getUsername()) || isBlank(user.getPassword())) {
            return ResponseEntity.badRequest().body("錯誤：帳號和密碼不可為空！");
        }

        // 檢查帳號是否重複
        if (userRepository.existsByUsername(user.getUsername())) {
            return ResponseEntity.badRequest().body("錯誤：該帳號已被註冊！");
        }

        // 密碼用 BCrypt 加密後再存入
        user.setPassword(jwtUtil.encodePassword(user.getPassword()));
        
        // 預設註冊進來的用戶權限都是普通 "USER"
        if (user.getRole() == null) {
            user.setRole("USER");
        }

        userRepository.save(user);
        return ResponseEntity.status(HttpStatus.CREATED).body("註冊成功！");
    }

    // 2. 用戶登入 API
    // POST http://localhost:8080/api/auth/login
    @PostMapping("/login")
    public ResponseEntity<?> login(@RequestBody User loginRequest) {
        // 根據帳號尋找用戶
        return userRepository.findByUsername(loginRequest.getUsername())
                .map(user -> {
                    // 比對密碼
                    if (jwtUtil.matchesPassword(loginRequest.getPassword(), user.getPassword())) {
                        // 密碼正確，簽發 Token
                        String token = jwtUtil.generateToken(user.getUsername(), user.getRole());
                        
                        // 打包成 JSON 回傳給前端
                        Map<String, String> response = new HashMap<>();
                        response.put("token", token);
                        response.put("username", user.getUsername());
                        response.put("role", user.getRole());
                        
                        return ResponseEntity.ok(response);
                    } else {
                        return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("錯誤：密碼不正確！");
                    }
                })
                .orElse(ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("錯誤：找不到該帳號！"));
    }

    private boolean isBlank(String value) {
        return value == null || value.trim().isEmpty();
    }
}
