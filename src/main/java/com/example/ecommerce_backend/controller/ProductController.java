package com.example.ecommerce_backend.controller;
import com.example.ecommerce_backend.model.Product;
import com.example.ecommerce_backend.repository.ProductRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PagedModel;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.Optional;

@RestController
@RequestMapping("/api/products")
public class ProductController {

    @Autowired
    private ProductRepository productRepository;

    // 1. 查詢商品列表（支援分頁，預設第 0 頁，每頁 10 筆）
    // 測試網址：GET http://localhost:8080/api/products?page=0&size=5
    @GetMapping
    public ResponseEntity<PagedModel<Product>> getAllProducts(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        Pageable pageable = PageRequest.of(page, size);
        Page<Product> products = productRepository.findByIsDeletedFalse(pageable);

        PagedModel<Product> pagedModel = new PagedModel<>(products);
        return ResponseEntity.ok(pagedModel);
    }

    // 2. 查詢單一商品詳情
    // 測試網址：GET http://localhost:8080/api/products/1
    @GetMapping("/{id}")
    public ResponseEntity<Product> getProductById(@PathVariable Long id) {
        Optional<Product> product = productRepository.findByIdAndIsDeletedFalse(id);
        return product.map(ResponseEntity::ok)
                      .orElse(ResponseEntity.notFound().build());
    }

    // 3. 新增商品
    // 測試網址：POST http://localhost:8080/api/products (Body 帶 JSON)
    @PostMapping
    public ResponseEntity<?> createProduct(@RequestBody Product product) {
        if (hasInvalidPrice(product)) {
            return ResponseEntity.badRequest().body("錯誤：商品價格不可為負數！");
        }

        Product savedProduct = productRepository.save(product);
        return ResponseEntity.ok(savedProduct);
    }

    // 4. 修改商品資訊
    // 測試網址：PUT http://localhost:8080/api/products/1 (Body 帶 JSON)
    @PutMapping("/{id}")
    public ResponseEntity<?> updateProduct(@PathVariable Long id, @RequestBody Product productDetails) {
        if (hasInvalidPrice(productDetails)) {
            return ResponseEntity.badRequest().body("錯誤：商品價格不可為負數！");
        }

        return productRepository.findByIdAndIsDeletedFalse(id).map(existingProduct -> {
            existingProduct.setName(productDetails.getName());
            existingProduct.setPrice(productDetails.getPrice());
            existingProduct.setStock(productDetails.getStock());
            Product updatedProduct = productRepository.save(existingProduct);
            return ResponseEntity.ok(updatedProduct);
        }).orElse(ResponseEntity.notFound().build());
    }

    // 5. 刪除商品（實作軟刪除邏輯，不真正刪除資料）
    // 測試網址：DELETE http://localhost:8080/api/products/1
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteProduct(@PathVariable Long id) {
        return productRepository.findByIdAndIsDeletedFalse(id).map(product -> {
            product.setIsDeleted(true); // 將軟刪除標記設為 true
            productRepository.save(product);
            return ResponseEntity.noContent().<Void>build(); // 回傳 204 No Content
        }).orElse(ResponseEntity.notFound().build());
    }

    private boolean hasInvalidPrice(Product product) {
        return product == null || product.getPrice() == null || product.getPrice().compareTo(BigDecimal.ZERO) < 0;
    }
}
