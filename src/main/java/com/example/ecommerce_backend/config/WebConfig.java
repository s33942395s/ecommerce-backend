package com.example.ecommerce_backend.config;

import com.example.ecommerce_backend.util.JwtInterceptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
public class WebConfig implements WebMvcConfigurer {

    @Autowired
    private JwtInterceptor jwtInterceptor;

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        // 看守商品管理的所有 API (/api/products/**)
        registry.addInterceptor(jwtInterceptor)
                .addPathPatterns("/api/products/**","/api/orders/**"); // 也看守訂單相關的 API，確保下單前必須登入;
    }
}