package com.example.zylo.auth.service;

import com.example.zylo.auth.dto.LoginRequest;
import com.example.zylo.auth.dto.LoginResponse;
import com.example.zylo.auth.dto.RefreshTokenRequest;
import com.example.zylo.auth.dto.RegisterRequest;
import com.example.zylo.common.dto.ApiResponse;
import com.example.zylo.user.entity.User;
import com.example.zylo.user.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class AuthServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private JwtService jwtService;

    @Mock
    private AuthenticationManager authenticationManager;

    @InjectMocks
    private AuthService authService;

    private RegisterRequest registerRequest;
    private LoginRequest loginRequest;
    private User testUser;

    @BeforeEach
    void setup() {
        registerRequest = new RegisterRequest();
        registerRequest.setName("John Doe");
        registerRequest.setEmail("john@example.com");
        registerRequest.setPassword("Test@1234");

        loginRequest = new LoginRequest();
        loginRequest.setEmail("john@example.com");
        loginRequest.setPassword("Test@1234");

        testUser = User.builder()
                .id(1L)
                .email("john@example.com")
                .name("John Doe")
                .passwordHash("$2a$12$dgH3J09qlfIDO1lXaGAqrOkLvuDpVfUVSw0.b/u77agsSKe9/1j26")
                .role(User.Role.CUSTOMER)
                .build();
    }

    @Test
    @DisplayName("Should register user successfully")
    void shouldRegisterUserSuccessfully() {
        // Given
        when(userRepository.existsByEmail(anyString())).thenReturn(false);
        when(passwordEncoder.encode(anyString())).thenReturn("$2a$12$dgH3J09qlfIDO1lXaGAqrOkLvuDpVfUVSw0.b/u77agsSKe9/1j26");
        when(userRepository.save(any(User.class))).thenReturn(testUser);

        // When
        ApiResponse<String> response = authService.register(registerRequest);

        // Then
        assertThat(response.isSuccess()).isTrue();
        assertThat(response.getMessage()).contains("Success");
        verify(userRepository).save(any(User.class));
        verify(passwordEncoder).encode("Test@1234");
    }

    @Test
    @DisplayName("Should throw exception when email already exists")
    void shouldThrowExceptionWhenEmailAlreadyExists() {
        // Given
        when(userRepository.existsByEmail("john@example.com")).thenReturn(true);

        // When/Then
        assertThatThrownBy(() -> authService.register(registerRequest))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Email already registered: john@example.com");

        verify(userRepository, never()).save(any());
    }

    @Test
    @DisplayName("Should Login User successfully")
    void shouldLoginUserSuccessfully() {
        // Given
        when(userRepository.findByEmail(anyString())).thenReturn(Optional.of(testUser));
        when(jwtService.generateAccessToken(any())).thenReturn("access.token.here");
        when(jwtService.generateRefreshToken(any())).thenReturn("refresh.token.here");
        when(jwtService.getJwtExpirationTime()).thenReturn(86400000L);

        // When
        LoginResponse response = authService.login(loginRequest);

        // Then
        assertThat(response).isNotNull();
        assertThat(response.getAccessToken()).isEqualTo("access.token.here");
        assertThat(response.getRefreshToken()).isEqualTo("refresh.token.here");
        assertThat(response.getUser().getEmail()).isEqualTo("john@example.com");

        verify(authenticationManager).authenticate(any(UsernamePasswordAuthenticationToken.class));
        verify(userRepository).save(any(User.class)); // last login at updated
    }

    @Test
    @DisplayName("Should throw exception for invalid credentials")
    void shouldThrowExceptionForInvalidCredentials() {
        // Given
        when(authenticationManager.authenticate(any()))
                .thenThrow(new BadCredentialsException("Invalid Credentials"));

        // When/Then
        assertThatThrownBy(() -> authService.login(loginRequest))
                .isInstanceOf(BadCredentialsException.class)
                .hasMessageContaining("Invalid");

        verify(jwtService, never()).generateAccessToken(any());
    }

    @Test
    @DisplayName("Should generate refresh token successfully")
    void shouldGenerateRefreshTokenSuccessfully() {
        // Given
        RefreshTokenRequest request = new RefreshTokenRequest();
        request.setRefreshToken("valid.refresh.token");

        when(jwtService.extractTokenType(anyString())).thenReturn("REFRESH");
        when(jwtService.extractEmail(anyString())).thenReturn("john@example.com");
        when(jwtService.extractUserId(anyString())).thenReturn(1L);
        when(jwtService.isRefreshTokenValid(anyString(), anyLong())).thenReturn(true);
        when(userRepository.findByEmail(anyString())).thenReturn(Optional.of(testUser));
        when(jwtService.generateAccessToken(any())).thenReturn("new.access.token");
        when(jwtService.getJwtExpirationTime()).thenReturn(86400000L);

        // When
        LoginResponse response = authService.refreshToken(request);

        // Then
        assertThat(response.getAccessToken()).isEqualTo("new.access.token");
        assertThat(response.getRefreshToken()).isEqualTo("valid.refresh.token");
    }

    @Test
    @DisplayName("Should reject invalid refresh token")
    void shouldRejectInvalidRefreshToken() {
        // Given
        RefreshTokenRequest request = new RefreshTokenRequest();
        request.setRefreshToken("invalid.token");

        when(jwtService.extractTokenType(anyString())).thenReturn("ACCESS"); // Wrong type

        // When/Then
        assertThatThrownBy(() -> authService.refreshToken(request))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Invalid Refresh Token");
    }
}
