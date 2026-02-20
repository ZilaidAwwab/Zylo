package com.example.zylo.auth.service;

import com.example.zylo.auth.dto.*;
import com.example.zylo.common.dto.ApiResponse;
import com.example.zylo.user.entity.User;
import com.example.zylo.user.repository.UserRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Slf4j
@Service
public class AuthService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final AuthenticationManager authenticationManager;
    private final RedisTemplate<String, Object> redisTemplate;

    // Constructor injection with @Lazy to prevent circular dependency
    @Autowired
    public AuthService(
            UserRepository userRepository,
            PasswordEncoder passwordEncoder,
            JwtService jwtService,
            @Lazy AuthenticationManager authenticationManager,
            RedisTemplate<String, Object> redisTemplate
    ) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtService = jwtService;
        this.authenticationManager = authenticationManager;
        this.redisTemplate = redisTemplate;
    }

    // Register
    @Transactional
    public ApiResponse<String> register(RegisterRequest request) {

        // 1. Check if the email already exists
        if (userRepository.existsByEmail(request.getEmail())) {
            throw new IllegalArgumentException("Email already registered: " + request.getEmail());
        }

        // 2. Build user entity
        User user = User.builder()
                .email(request.getEmail())
                .name(request.getName())
                .phoneNo(request.getPhoneNo())
                // Hashing the password before saving
                .passwordHash(passwordEncoder.encode(request.getPassword()))
                .role(User.Role.CUSTOMER)   // Default Role
                .emailVerified(false)
                .build();

        // 3. Saving the user in the DB
        userRepository.save(user);

        log.debug("New user registered: {}", request.getEmail());

        return ApiResponse.success("Registration successful.");
    }

    // Login
    @Transactional
    public LoginResponse login(LoginRequest request) {

        // 1. Spring Security will validate credentials first
        // This calls CustomerDetailsService.loadUserByUsername() and compares password hash
        // (Throws exception if the credentials are wrong)
        try {
            authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(request.getEmail(), request.getPassword())
            );
        } catch (BadCredentialsException e) {
            throw new BadCredentialsException("Invalid email or password");
        }

        // 2. Load user from DB
        User user = userRepository.findByEmail(request.getEmail()).orElseThrow(() -> new UsernameNotFoundException("User not found"));

        // 3. Generate tokens
        String accessToken = jwtService.generateAccessToken(user);
        String refreshToken = jwtService.generateRefreshToken(user);

        // 4. Update last login timestamp
        user.setLastLoginAt(LocalDateTime.now());
        userRepository.save(user);

        log.info("User logged in: {} | role: {}", user.getEmail(), user.getRole());

        // 5. Return token (and user info)
        return LoginResponse.builder()
                .accessToken(accessToken)
                .refreshToken(refreshToken)
                .tokenType("Bearer")
                .expiresIn(jwtService.getJwtExpirationTime())
                .user(LoginResponse.UserInfo.builder()
                        .id(user.getId())
                        .email(user.getEmail())
                        .name(user.getName())
                        .role(user.getRole().name())
                        .build())
                .build();
    }

    // Refresh Token
    public LoginResponse refreshToken(RefreshTokenRequest request) {

        String refreshToken = request.getRefreshToken();

        // 1. Validate token type
        String tokenType = jwtService.extractTokenType(refreshToken);
        if (!"REFRESH".equals(tokenType)) {
            throw new IllegalArgumentException("Invalid Refresh Token");
        }

        // If the token is valid (Refresh Token), then fetch the user
        // 2. Get user from token
        String email = jwtService.extractEmail(refreshToken);
        Long id = jwtService.extractUserId(refreshToken);

        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new UsernameNotFoundException("User not found"));

        // 3. Validate refresh token exists in Redis
        if (!jwtService.isRefreshTokenValid(refreshToken, id)) {
            throw new IllegalArgumentException("Refresh token expired or invalid. Please login again");
        }

        // 4. Generate new access token
        String newAccessToken = jwtService.generateAccessToken(user);

        log.info("Token refreshed for user: {}", email);

        return LoginResponse.builder()
                .accessToken(newAccessToken)
                .refreshToken(refreshToken)     // Same refresh token
                .tokenType("Bearer")
                .expiresIn(jwtService.getJwtExpirationTime())
                .user(LoginResponse.UserInfo.builder()
                        .id(user.getId())
                        .email(user.getEmail())
                        .name(user.getName())
                        .role(user.getRole().name())
                        .build())
                .build();
    }

    // Logout
    @Transactional
    public ApiResponse<String> logout(String accessToken, Long userId) {

        // 1. Blacklist the access token in Redis (so it can't be used, even if it is not expired)
        jwtService.blacklistToken(accessToken);

        // 2. Invalidate refresh token in Redis
        jwtService.invalidateRefreshToken(userId);

        // 3. Clear user from Redis cache
        String email = jwtService.extractEmail(accessToken);
        redisTemplate.delete("user:session:" + email);

        log.info("User logged out, tokens invalidated for userId: {}", userId);

        return ApiResponse.success("Logged out successfully");
    }

    // Promote to Admin
    @Transactional
    public ApiResponse<String> promoteToAdmin(String email) {

        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new UsernameNotFoundException("User not found: " + email));

        if (user.getRole() == User.Role.ADMIN) {
            return ApiResponse.error("User is already Admin");
        }

        user.setRole(User.Role.ADMIN);
        userRepository.save(user);

        // Clear Redis cache, so new role takes effect immediately
        redisTemplate.delete("user:session:" + email);

        log.info("User promoted to Admin: {}", email);
        return ApiResponse.success("User " + email + " promoted to Admin");
    }
}
