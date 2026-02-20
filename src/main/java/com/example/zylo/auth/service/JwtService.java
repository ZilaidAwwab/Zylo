package com.example.zylo.auth.service;

import com.example.zylo.user.entity.User;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import javax.crypto.SecretKey;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.function.Function;

@Slf4j
@Service
public class JwtService {

    @Value("${jwt.secret}")
    private String secretKey;

    @Value("${jwt.expiration}")
    private Long jwtExpiration;

    @Value("${jwt.refresh-expiration}")
    private Long refreshExpiration;

    private final RedisTemplate<String, String> redisTemplate;

    public JwtService(@Qualifier("jwtRedisTemplate") RedisTemplate<String, String> redisTemplate) {
        this.redisTemplate = redisTemplate;
    }

    // Generate Access Token
    public String generateAccessToken(User user) {
        Map<String, Object> claims = new HashMap<>();

        // claims inside the JWT payload
        claims.put("userId", user.getId());
        claims.put("email", user.getEmail());
        claims.put("role", user.getRole().name());
        claims.put("name", user.getName());
        claims.put("type", "ACCESS");

        return buildToken(claims, user.getEmail(), jwtExpiration);
    }

    // Generate Refresh Token
    public String generateRefreshToken(User user) {
        Map<String, Object> claims = new HashMap<>();
        claims.put("userId", user.getId());
        claims.put("type", "REFRESH");

        String refreshToken = buildToken(claims, user.getEmail(), refreshExpiration);

        // Storing refresh token in redis
        // Key: "refresh_token:{userId}"
        // This lets us invalidate specific user's token on logout
        String redisKey = "refresh_token: " + user.getId();
        redisTemplate.opsForValue().set(
                redisKey,
                refreshToken,
                refreshExpiration,
                TimeUnit.MILLISECONDS
        );

        return refreshToken;
    }

    // Token Builder
    public String buildToken(Map<String, Object> claims, String subject, Long expiration) {
        return Jwts.builder()
                .claims(claims)
                .subject(subject)   // email
                .issuedAt(new Date())
                .expiration(new Date(System.currentTimeMillis() + expiration))
                .signWith(getSigningKey())  // HS256 by default
                .compact();
    }

    // Validate Token
    public boolean isTokenValid(String token, String email) {
        try {
            String extractedEmail = extractEmail(token);
            boolean notExpired = !isTokenExpired(token);
            boolean emailMatches = extractedEmail.equals(email);

            // Checking if the token is blacklisted (logged out)
            boolean notBlackListed = !isTokenBlacklisted(token);

            // Should not be expired, email mismatch and blacklisted (logged out)
            return notExpired && emailMatches && notBlackListed;

        } catch (JwtException e) {
            log.warn("JWT validation failed: {}", e.getMessage());
            return false;
        }
    }

    // Helper methods
    public String extractEmail(String token) {
        return extractClaim(token, Claims::getSubject);
    }

    public Long extractUserId(String token) {
        return extractClaim(token, claims ->
                claims.get("userId", Long.class));
    }

    public String extractRole(String token) {
        return extractClaim(token, claims ->
                claims.get("role", String.class));
    }

    public String extractTokenType(String token) {
        return extractClaim(token, claims ->
                claims.get("type", String.class));
    }

    public Date extractExpiration(String token) {
        return extractClaim(token, Claims::getExpiration);
    }

    // Generic claim extractor
    public <T> T extractClaim(String token, Function<Claims, T> claimResolver) {
        Claims claims = extractAllClaims(token);
        // claimResolver function will return the intended claim demanded in the function call
        return claimResolver.apply(claims);
    }

    private Claims extractAllClaims(String token) {
        return Jwts.parser()
                .verifyWith(getSigningKey())
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }

    private boolean isTokenExpired(String token) {
        return extractExpiration(token).before(new Date());
    }

    // Blacklist Token (logout)
    // When the user logs out, add token to Redis Blacklist
    // So, even if the token is valid, it won't work
    public void blacklistToken(String token) {
        String email = extractEmail(token);
        Date expiry = extractExpiration(token);

        // ttl = time to live
        long ttl = expiry.getTime() - System.currentTimeMillis();
        if (ttl > 0) {
            // Stores in redis until naturally expires
            String blacklistKey = "blacklist: " + token;
            redisTemplate.opsForValue().set(
                    blacklistKey,
                    email,
                    ttl,
                    TimeUnit.MILLISECONDS
            );
            log.info("Token blacklisted for user: {}", email);
        }
    }

    public boolean isTokenBlacklisted(String token) {
        String blacklistKey = "blacklist: " + token;
        return Boolean.TRUE.equals(redisTemplate.hasKey(blacklistKey));
    }

    // Invalidate refresh token (Logout)
    public void invalidateRefreshToken(Long userId) {
        String redisKey = "refresh_token: " + userId;
        redisTemplate.delete(redisKey);
        log.info("Refresh token invalidated for userId: {}", userId);
    }

    public boolean isRefreshTokenValid(String token, Long userId) {
        String redisKey = "refresh_token: " + userId;
        String storedToken = redisTemplate.opsForValue().get(redisKey);
        return token.equals(storedToken) && isTokenExpired(token);
    }

    // Signing Key
    private SecretKey getSigningKey() {
        byte[] keyBytes = Decoders.BASE64.decode(secretKey);
        return Keys.hmacShaKeyFor(keyBytes);
    }

    public Long getJwtExpirationTime() {
        return jwtExpiration;
    }
}
