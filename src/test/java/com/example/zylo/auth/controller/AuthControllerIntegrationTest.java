package com.example.zylo.auth.controller;

import com.example.zylo.auth.dto.LoginRequest;
import com.example.zylo.auth.dto.RegisterRequest;
import com.example.zylo.user.entity.User;
import com.example.zylo.user.repository.UserRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.MediaType;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import static org.hamcrest.Matchers.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
public class AuthControllerIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    private ObjectMapper objectMapper = new ObjectMapper();

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    private RegisterRequest registerRequest;
    private LoginRequest loginRequest;

    @BeforeEach
    void setup() {
        userRepository.deleteAll();

        registerRequest = new RegisterRequest();
        registerRequest.setName("John Miller");
        registerRequest.setEmail("miller@example.com");
        registerRequest.setPassword("Test@1234");

        loginRequest = new LoginRequest();
        loginRequest.setEmail("miller@example.com");
        loginRequest.setPassword("Test@1234");
    }

    @Test
    @DisplayName("POST /auth/register - Should register user successfully")
    void shouldRegisterUserSuccessfully() throws Exception {
        mockMvc.perform(post("/auth/register")
                .contentType(String.valueOf(MediaType.APPLICATION_JSON))
                .content(objectMapper.writeValueAsString(registerRequest)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.message").value(containsString("Success")));
    }

    @Test
    @DisplayName("POST /auth/register - Should reject duplicate email")
    void shouldRejectDuplicateEmail() throws Exception {
        // Register once
        mockMvc.perform(post("/auth/register")
                .contentType(String.valueOf(MediaType.APPLICATION_JSON))
                .content(objectMapper.writeValueAsString(registerRequest)))
                .andExpect(status().isCreated());

        // Try to register again with the same email
        mockMvc.perform(post("/auth/register")
                .contentType(String.valueOf(MediaType.APPLICATION_JSON))
                .content(objectMapper.writeValueAsString(registerRequest)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.message").value(containsString("Email already registered: miller@example.com")));

    }

    @Test
    @DisplayName("POST /auth/register - Should validate email format")
    void shouldValidateEmailFormat() throws Exception {
        registerRequest.setEmail("invalid-email");

        mockMvc.perform(post("/auth/register")
                .contentType(String.valueOf(MediaType.APPLICATION_JSON))
                .content(objectMapper.writeValueAsString(registerRequest)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value(containsString("Validation failed")))
                .andExpect(jsonPath("$.data.email").exists());
    }

    @Test
    @DisplayName("POST /auth/register - Should validate password strength")
    void shouldValidatePasswordStrength() throws Exception {
        registerRequest.setPassword("weak");

        mockMvc.perform(post("/auth/register")
                .contentType(String.valueOf(MediaType.APPLICATION_JSON))
                .content(objectMapper.writeValueAsString(registerRequest)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.data.password").exists());
    }

    @Test
    @DisplayName("POST /auth/login - Should login successfully")
    void shouldLoginSuccessfully() throws Exception {
        // Create user first
        User user = User.builder()
                .email("miller@example.com")
                .name("John Miller")
                .passwordHash(passwordEncoder.encode("Test@1234"))
                .role(User.Role.CUSTOMER)
                .emailVerified(true)
                .build();
        userRepository.save(user);

        // Login
        mockMvc.perform(post("/auth/login")
                .contentType(String.valueOf(MediaType.APPLICATION_JSON))
                .content(objectMapper.writeValueAsString(loginRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.accessToken").exists())
                .andExpect(jsonPath("$.data.refreshToken").exists())
                .andExpect(jsonPath("$.data.user.email").value("miller@example.com"))
                .andExpect(jsonPath("$.data.user.role").value("CUSTOMER"));
    }

    @Test
    @DisplayName("POST /auth/login - Should reject wrong password")
    void shouldRejectWrongPassword() throws Exception {
        // Create user first
        User user = User.builder()
                .email("max@example.com")
                .name("Max Schwarz")
                .passwordHash(passwordEncoder.encode("Test@1234"))
                .role(User.Role.CUSTOMER)
                .emailVerified(true)
                .build();
        userRepository.save(user);

        // Try with wrong password
        loginRequest.setPassword("WrongPassword");

        mockMvc.perform(post("/auth/login")
                .contentType(String.valueOf(MediaType.APPLICATION_JSON))
                .content(objectMapper.writeValueAsString(loginRequest)))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.message").value(containsString("Invalid")));
    }

    @Test
    @DisplayName("POST /auth/me - Should return user info with valid token")
    void shouldReturnUserInfoWithValidToken() throws Exception {
        // Create user first
        User user = User.builder()
                .email("miller@example.com")
                .name("Max Schwarz")
                .passwordHash(passwordEncoder.encode("Test@1234"))
                .role(User.Role.CUSTOMER)
                .emailVerified(true)
                .build();
        userRepository.save(user);

        // Login to get token
        String loginResponse = mockMvc.perform(post("/auth/login")
                .contentType(String.valueOf(MediaType.APPLICATION_JSON))
                .content(objectMapper.writeValueAsString(loginRequest)))
                .andReturn()
                .getResponse()
                .getContentAsString();

        String token = objectMapper.readTree(loginResponse)
                .get("data")
                .get("accessToken")
                .asText();

        // Call /auth/me with token
        mockMvc.perform(post("/auth/me")
                .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.email").value("miller@example.com"))
                .andExpect(jsonPath("$.data.role").value("CUSTOMER"));
    }

    @Test
    @DisplayName("POST /auth/me - Should reject request without token")
    void shouldRejectRequestWithoutToken() throws Exception {
        mockMvc.perform(post("/auth/me"))
                .andExpect(status().isForbidden());
    }
}
