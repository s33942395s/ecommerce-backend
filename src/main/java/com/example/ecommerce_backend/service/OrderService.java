package com.example.ecommerce_backend.service;

import com.example.ecommerce_backend.dto.OrderRequest;
import com.example.ecommerce_backend.model.*;
import com.example.ecommerce_backend.repository.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

@Service
public class OrderService {

    @Autowired
    private OrderRepository orderRepository;

    @Autowired
    private ProductRepository productRepository;

    @Autowired
    private UserRepository userRepository;

    @Transactional
    public Order createOrder(OrderRequest request, String username) {
        if (request == null || request.getItems() == null || request.getItems().isEmpty()) {
            throw new RuntimeException("錯誤：訂單商品不可為空！");
        }

        // 1. 尋找下單用戶
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new RuntimeException("錯誤：找不到該用戶！"));

        Order order = new Order();
        order.setUser(user);
        order.setStatus("PENDING"); // 初始狀態：待付款

        List<OrderItem> orderItems = new ArrayList<>();
        BigDecimal totalAmount = BigDecimal.ZERO;

        // 2. 處理每一筆下單的商品
        for (OrderRequest.Item itemRequest : request.getItems()) {
            if (itemRequest == null || itemRequest.getQuantity() == null || itemRequest.getQuantity() <= 0) {
                throw new RuntimeException("錯誤：商品數量必須大於 0！");
            }

            Product product = productRepository.findByIdAndIsDeletedFalse(itemRequest.getProductId())
                    .orElseThrow(() -> new RuntimeException("錯誤：商品 ID " + itemRequest.getProductId() + " 已下架或不存在！"));

            // 【扣庫存】
            int updatedRows = productRepository.deductStockIfEnough(product.getId(), itemRequest.getQuantity());
            if (updatedRows == 0) {
                throw new RuntimeException("錯誤：商品 " + product.getName() + " 庫存不足！");
            }

            // 建立訂單明細
            OrderItem orderItem = new OrderItem();
            orderItem.setProduct(product);
            orderItem.setQuantity(itemRequest.getQuantity());
            orderItem.setPrice(product.getPrice()); // 紀錄成交當下價格
            orderItems.add(orderItem);

            // 累加總金額
            BigDecimal itemTotal = product.getPrice().multiply(BigDecimal.valueOf(itemRequest.getQuantity()));
            totalAmount = totalAmount.add(itemTotal);
        }

        order.setOrderItems(orderItems);
        order.setTotalAmount(totalAmount);

        // 3. 儲存訂單
        return orderRepository.save(order);
    }
}
