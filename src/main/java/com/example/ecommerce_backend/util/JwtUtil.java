package com.example.ecommerce_backend.util;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Date;

@Component
public class JwtUtil {

    
    // 當這個元件被建立時，自動把設定檔裡的 app.jwt.secret 注入進來    
    private final SecretKey key ;
    public JwtUtil(@Value("${app.jwt.secret}") String secretString) {
        this.key = Keys.hmacShaKeyFor(secretString.getBytes(StandardCharsets.UTF_8));
    }
    
    private final BCryptPasswordEncoder passwordEncoder = new BCryptPasswordEncoder();

    public String encodePassword(String rawPassword) {
        return passwordEncoder.encode(rawPassword);
    }

    public boolean matchesPassword(String rawPassword, String encodedPassword) {
        return passwordEncoder.matches(rawPassword, encodedPassword);
    }

    // 簽發 Token
    public String generateToken(String username, String role) {
        long expirationTime = 86400000; // 24小時
        return Jwts.builder()
                .subject(username)
                .claim("role", role)
                .issuedAt(new Date())
                .expiration(new Date(System.currentTimeMillis() + expirationTime))
                .signWith(key) // 使用固定的 key 簽名
                .compact();
    }

    // 解析 Token
    public Claims extractAllClaims(String token) {
        return Jwts.parser()
                .verifyWith(key) // 使用同一個固定的 key 解密驗證
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }

    public String extractRole(String token) {
        return extractAllClaims(token).get("role", String.class);
    }
}