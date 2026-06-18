package com.example.ecommerce_backend.util;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

@Component
public class JwtInterceptor implements HandlerInterceptor {

    @Autowired
    private JwtUtil jwtUtil;

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler)
            throws Exception {
        // 1. 如果是 GET 請求（看商品），直接放行，不需要 Token
        if ("GET".equalsIgnoreCase(request.getMethod())) {
            return true;
        }

        // 2. 如果是 POST/PUT/DELETE，從請求標頭拿 Authorization
        String authHeader = request.getHeader("Authorization");

        // 3. 檢查格式是否符合標準的 "Bearer your_token_here"
        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
            response.setContentType("application/json;charset=UTF-8");
            response.setCharacterEncoding("UTF-8");
            response.getWriter().write("錯誤：拒絕存取！請先登入並攜帶 Token。");
            return false;
        }

        // 4. 拔掉 "Bearer " 這 7 個字，純化出 token
        String token = authHeader.substring(7);

        try {
            // 5. 解析 Token 並檢查是不是 ADMIN 權限
            String role = jwtUtil.extractRole(token);
            String uri = request.getRequestURI();

            if (uri.contains("/api/orders")) {
                return true; // 訂單相關的 API 只要登入即可，不需要特別檢查角色
            }

            if (uri.contains("/api/products")){
                if ("ADMIN".equalsIgnoreCase(role)) {
                    return true; // 是管理員，准許通過！
                } else {
                    response.setStatus(HttpServletResponse.SC_FORBIDDEN);
                    response.setContentType("application/json;charset=UTF-8");
                    response.setCharacterEncoding("UTF-8");
                    response.getWriter().write("錯誤：權限不足！只有管理員(ADMIN)可以操作商品。");
                    return false;
                }
            }

            return true; // 其他 API 先放行，未來可以再加更多細部檢查
        } catch (Exception e) {
            // Token 過期或被竄改時會噴異常
            response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
            response.setContentType("application/json;charset=UTF-8");
            response.setCharacterEncoding("UTF-8");
            response.getWriter().write("錯誤：無效或已過期的 Token！");
            return false;
        }
    }
}