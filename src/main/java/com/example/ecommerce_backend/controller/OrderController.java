package com.example.ecommerce_backend.controller;

import com.example.ecommerce_backend.dto.OrderRequest;
import com.example.ecommerce_backend.model.Order;
import com.example.ecommerce_backend.service.OrderService;
import com.example.ecommerce_backend.util.JwtUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/orders")
public class OrderController {

    @Autowired
    private OrderService orderService;

    @Autowired
    private JwtUtil jwtUtil;

    // 建立訂單 (必須登入)
    // POST http://localhost:8080/api/orders
    @PostMapping
    public ResponseEntity<?> placeOrder(
            @RequestBody OrderRequest orderRequest,
            @RequestHeader("Authorization") String authHeader) {
        
        try {
            // 從 Header 提取 Token 並解密出用戶名
            String token = authHeader.substring(7);
            String username = jwtUtil.extractAllClaims(token).getSubject(); // 從 Token 主體抽取 username

            Order order = orderService.createOrder(orderRequest, username);
            return ResponseEntity.ok(order);
            
        } catch (org.springframework.orm.ObjectOptimisticLockingFailureException e) {
           
            return ResponseEntity.status(409).body("錯誤：系統忙碌，請重新嘗試提交訂單！");
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }
}