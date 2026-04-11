package com.lmi.crm.config;

import com.lmi.crm.entity.User;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.MalformedJwtException;
import io.jsonwebtoken.security.Keys;
import io.jsonwebtoken.security.SignatureException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Date;

@Slf4j
@Component
public class JwtUtil {

    @Value("${jwt.secret}")
    private String secret;

    @Value("${jwt.expiry.hours}")
    private long expiryHours;

    private SecretKey getSigningKey() {
        return Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
    }

    public String generateToken(User user) {
        Date expiry = new Date(System.currentTimeMillis() + expiryHours * 3600 * 1000L);

        String token = Jwts.builder()
                .claim("userId", user.getId())
                .claim("role", user.getRole().name())
                .expiration(expiry)
                .signWith(getSigningKey())
                .compact();

        log.debug("JWT generated — userId: {}, role: {}, expiresAt: {}", user.getId(), user.getRole(), expiry);
        return token;
    }

    public Integer extractUserId(String token) {
        Integer userId = extractClaims(token).get("userId", Integer.class);
        log.debug("JWT extractUserId — userId: {}", userId);
        return userId;
    }

    public String extractRole(String token) {
        String role = extractClaims(token).get("role", String.class);
        log.debug("JWT extractRole — role: {}", role);
        return role;
    }

    public boolean isValid(String token) {
        try {
            extractClaims(token);
            log.debug("JWT validation passed");
            return true;
        } catch (ExpiredJwtException e) {
            log.warn("JWT validation failed — token expired: {}", e.getMessage());
        } catch (SignatureException e) {
            log.warn("JWT validation failed — invalid signature: {}", e.getMessage());
        } catch (MalformedJwtException e) {
            log.warn("JWT validation failed — malformed token: {}", e.getMessage());
        } catch (Exception e) {
            log.warn("JWT validation failed — {}: {}", e.getClass().getSimpleName(), e.getMessage());
        }
        return false;
    }

    private Claims extractClaims(String token) {
        return Jwts.parser()
                .verifyWith(getSigningKey())
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }
}
