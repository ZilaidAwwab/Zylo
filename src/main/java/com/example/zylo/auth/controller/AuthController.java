package com.example.zylo.auth.controller;

import com.example.zylo.auth.dto.LoginRequest;
import com.example.zylo.auth.dto.LoginResponse;
import com.example.zylo.auth.dto.RefreshTokenRequest;
import com.example.zylo.auth.dto.RegisterRequest;
import com.example.zylo.auth.service.AuthService;
import com.example.zylo.auth.service.JwtService;
import com.example.zylo.common.dto.ApiResponse;
import com.example.zylo.user.entity.User;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;
    private final JwtService jwtService;

    // Register
    @PostMapping("/register")
    public ResponseEntity<ApiResponse<String>> register(
            @Valid @RequestBody RegisterRequest request
            ) {
        ApiResponse<String> response = authService.register(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    // Login
    @PostMapping("/login")
    public ResponseEntity<ApiResponse<LoginResponse>> login(
            @Valid @RequestBody LoginRequest request
            ) {
        LoginResponse loginResponse = authService.login(request);
        return ResponseEntity.ok(ApiResponse.success("Login Successful", loginResponse));
    }

    // Refresh
    @PostMapping("/refresh")
    public ResponseEntity<ApiResponse<LoginResponse>> refresh(
            @Valid @RequestBody RefreshTokenRequest request
            ) {
        LoginResponse response = authService.refreshToken(request);
        return ResponseEntity.ok(ApiResponse.success("Token Refreshed", response));
    }

    // Logout
    @PostMapping("/logout")
    public ResponseEntity<ApiResponse<String>> logout(
            @RequestHeader("Authorization") String bearerToken,
            @AuthenticationPrincipal User user
    ) {
        String token = bearerToken.substring(7);
        ApiResponse<String> response = authService.logout(token, user.getId());
        return ResponseEntity.ok(response);
    }

    @PostMapping("/me")
    public ResponseEntity<ApiResponse<LoginResponse.UserInfo>> me(
            @AuthenticationPrincipal User user
    ) {
        LoginResponse.UserInfo userInfo = LoginResponse.UserInfo.builder()
                .id(user.getId())
                .email(user.getEmail())
                .name(user.getName())
                .role(user.getRole().name())
                .build();

        return ResponseEntity.ok(ApiResponse.success(userInfo));
    }

    @PostMapping("/admin/promote")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<String>> promoteToAdmin(
            @RequestParam String email
    ) {
        ApiResponse<String> response = authService.promoteToAdmin(email);
        return ResponseEntity.ok(response);
    }
}
