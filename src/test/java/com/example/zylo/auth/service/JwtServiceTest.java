package com.example.zylo.auth.service;

import com.example.zylo.user.entity.User;
import io.jsonwebtoken.MalformedJwtException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.ValueOperations;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class JwtServiceTest {

    @Mock
    private RedisTemplate<String, String> jwtRedisTemplate;

    @Mock
    private ValueOperations<String, String> valueOperations;

    @InjectMocks
    private JwtService jwtService;

    private User testUser;

    @BeforeEach
    void setUp() {
        // Set test values using reflection (since @Value won't work in tests)
        ReflectionTestUtils.setField(jwtService, "secretKey",
                "404E635266556A586E3272357538782F413F4428472B4B6250645367566B5970");
        ReflectionTestUtils.setField(jwtService, "jwtExpiration", 86400000L);
        ReflectionTestUtils.setField(jwtService, "refreshExpiration", 2592000000L);

        // Mock Redis Operations
        // lenient().when(jwtRedisTemplate.opsForValue()).thenReturn(valueOperations);

        // Create Test User
        testUser = User.builder()
                .id(1L)
                .email("test@example.com")
                .name("Test User")
                .role(User.Role.CUSTOMER)
                .build();
    }

    @Test
    @DisplayName("Should generate valid access token")
    void shouldGenerateValidAccessToken() {
        // When
        String token = jwtService.generateAccessToken(testUser);

        // Then
        assertThat(token).isNotNull();
        assertThat(token.split("\\.")).hasSize(3); // JWT has 3 parts

        // Verify token contents
        String email = jwtService.extractEmail(token);
        Long userId = jwtService.extractUserId(token);
        String role = jwtService.extractRole(token);
        String tokenType = jwtService.extractTokenType(token);

        assertThat(email).isEqualTo("test@example.com");
        assertThat(userId).isEqualTo(1L);
        assertThat(role).isEqualTo("CUSTOMER");
        assertThat(tokenType).isEqualTo("ACCESS");
    }

    @Test
    @DisplayName("Should generate refresh token and store in Redis")
    void shouldGenerateRefreshTokenAndStoreInRedis() {
        // Setup Redis mock for this test
        when(jwtRedisTemplate.opsForValue()).thenReturn(valueOperations);

        // When
        String token = jwtService.generateRefreshToken(testUser);

        // Then
        assertThat(token).isNotNull();
        verify(valueOperations).set(
                eq("refresh_token:1"),
                eq(token),
                eq(2592000000L),
                eq(TimeUnit.MILLISECONDS)
        );
    }

    @Test
    @DisplayName("Should validate token successfully")
    void shouldValidateTokenSuccessfully() {
        // Given
        String token = jwtService.generateAccessToken(testUser);
        when(jwtRedisTemplate.hasKey(anyString())).thenReturn(false); // Not Blacklisted

        // When
        boolean isValid = jwtService.isTokenValid(token, "test@example.com");

        // Then
        assertThat(isValid).isTrue();
    }

    @Test
    @DisplayName("Should reject token with wrong email")
    void shouldRejectTokenWithWrongEmail() {
        // Given
        String token = jwtService.generateAccessToken(testUser);

        // When
        boolean isValid = jwtService.isTokenValid(token, "wrong@example.com");

        // Then
        assertThat(isValid).isFalse();
    }

    @Test
    @DisplayName("Should reject blacklisted token")
    void shouldRejectBlacklistedToken() {
        // Given
        String token = jwtService.generateAccessToken(testUser);
        when(jwtRedisTemplate.hasKey("blacklist:" + token)).thenReturn(true);

        // When
        boolean isValid = jwtService.isTokenValid(token, "test@example.com");

        // Then
        assertThat(isValid).isFalse();
    }

    @Test
    @DisplayName("Should blacklist token on logout")
    void shouldBlacklistTokenOnLogout() {
        // Redis mock
        when(jwtRedisTemplate.opsForValue()).thenReturn(valueOperations);

        // Given
        String token = jwtService.generateAccessToken(testUser);

        // When
        jwtService.blacklistToken(token);

        // Then
        verify(valueOperations).set(
                eq("blacklist:" + token),
                eq("test@example.com"),
                anyLong(),
                eq(TimeUnit.MILLISECONDS)
        );
    }

    @Test
    @DisplayName("Should throw exception for malformed token")
    void shouldThrowExceptionForMalformedToken() {
        // Given
        String malformedToken = "not.a.jwt.token";

        // When/Then
        assertThatThrownBy(() -> jwtService.extractEmail(malformedToken))
                .isInstanceOf(MalformedJwtException.class);
    }
}
