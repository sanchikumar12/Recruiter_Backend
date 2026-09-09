package com.pi.auth.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.pi.auth.dto.ActivateAccountRequest;
import com.pi.auth.dto.AuthResponse;
import com.pi.auth.dto.ForgotPasswordRequest;
import com.pi.auth.dto.LoginRequest;
import com.pi.auth.dto.RefreshTokenRequest;
import com.pi.auth.dto.RegisterRequest;
import com.pi.auth.dto.RegisterResponse;
import com.pi.auth.dto.ResetPasswordRequest;
import com.pi.auth.exception.AuthException;
import com.pi.auth.exception.GlobalExceptionHandler;
import com.pi.auth.exception.InvalidTokenException;
import com.pi.auth.service.AuthService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@ExtendWith(MockitoExtension.class)
class AuthControllerTest {

    private MockMvc mockMvc;
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Mock
    private AuthService authService;

    @InjectMocks
    private AuthController authController;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(authController)
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();
    }

    @Test
    @DisplayName("POST /api/v1/auth/register should return 201 with generated password")
    void register_Success() throws Exception {
        RegisterRequest request = new RegisterRequest("candidate@366pi.com", null);
        RegisterResponse response = new RegisterResponse(
                "User registered successfully. Temporary login credentials have been generated.",
                "candidate@366pi.com",
                "K9#mX2$pQ7!v",
                "ACTIVE"
        );
        when(authService.register(any())).thenReturn(response);

        mockMvc.perform(post("/api/v1/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.email").value("candidate@366pi.com"))
                .andExpect(jsonPath("$.generatedPassword").value("K9#mX2$pQ7!v"))
                .andExpect(jsonPath("$.status").value("ACTIVE"));
    }

    @Test
    @DisplayName("POST /api/v1/auth/register should return 409 when email already registered")
    void register_Conflict_Returns409() throws Exception {
        RegisterRequest request = new RegisterRequest("existing@366pi.com", "SecurePass123!");
        when(authService.register(any())).thenThrow(new AuthException("EMAIL_ALREADY_REGISTERED", "Email already registered"));

        mockMvc.perform(post("/api/v1/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("EMAIL_ALREADY_REGISTERED"));
    }

    @Test
    @DisplayName("POST /api/v1/auth/register should return 400 on invalid email")
    void register_ValidationFailure_Returns400() throws Exception {
        RegisterRequest request = new RegisterRequest("invalid-email", "short");

        mockMvc.perform(post("/api/v1/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VALIDATION_FAILED"));
    }

    @Test
    @DisplayName("POST /api/v1/auth/login should return 200 with tokens")
    void login_Success() throws Exception {
        LoginRequest request = new LoginRequest("candidate@366pi.com", "SecurePass123!");
        AuthResponse response = new AuthResponse("access.token.jwt", "refresh.token.uuid", "Bearer", 900);

        when(authService.login(any())).thenReturn(response);

        mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.accessToken").value("access.token.jwt"))
                .andExpect(jsonPath("$.refreshToken").value("refresh.token.uuid"))
                .andExpect(jsonPath("$.tokenType").value("Bearer"))
                .andExpect(jsonPath("$.expiresIn").value(900));
    }

    @Test
    @DisplayName("POST /api/v1/auth/login should return 401 on invalid credentials")
    void login_InvalidCredentials_Returns401() throws Exception {
        LoginRequest request = new LoginRequest("candidate@366pi.com", "WrongPassword!");
        when(authService.login(any())).thenThrow(new AuthException("INVALID_CREDENTIALS", "Invalid credentials"));

        mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("INVALID_CREDENTIALS"));
    }

    @Test
    @DisplayName("POST /api/v1/auth/login should return 423 when account is locked")
    void login_LockedAccount_Returns423() throws Exception {
        LoginRequest request = new LoginRequest("candidate@366pi.com", "Password123!");
        when(authService.login(any())).thenThrow(new AuthException("ACCOUNT_LOCKED", "Account is temporarily locked."));

        mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isLocked())
                .andExpect(jsonPath("$.code").value("ACCOUNT_LOCKED"));
    }

    @Test
    @DisplayName("POST /api/v1/auth/forgot-password should return 200 with reset token")
    void forgotPassword_Success() throws Exception {
        ForgotPasswordRequest request = new ForgotPasswordRequest("candidate@366pi.com");
        when(authService.forgotPassword(any())).thenReturn("reset-token-abc");

        mockMvc.perform(post("/api/v1/auth/forgot-password")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.resetToken").value("reset-token-abc"));
    }

    @Test
    @DisplayName("POST /api/v1/auth/reset-password should return 200 on success")
    void resetPassword_Success() throws Exception {
        ResetPasswordRequest request = new ResetPasswordRequest("reset-token-abc", "NewPassword123!");

        mockMvc.perform(post("/api/v1/auth/reset-password")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").exists());

        verify(authService).resetPassword(any());
    }

    @Test
    @DisplayName("POST /api/v1/auth/refresh should return 200 with rotated tokens")
    void refresh_Success() throws Exception {
        RefreshTokenRequest request = new RefreshTokenRequest("valid-refresh-token");
        AuthResponse response = new AuthResponse("new.access.token", "new.refresh.token", "Bearer", 900);

        when(authService.refresh(any())).thenReturn(response);

        mockMvc.perform(post("/api/v1/auth/refresh")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.accessToken").value("new.access.token"))
                .andExpect(jsonPath("$.refreshToken").value("new.refresh.token"));
    }

    @Test
    @DisplayName("POST /api/v1/auth/logout should return 200")
    void logout_Success() throws Exception {
        RefreshTokenRequest request = new RefreshTokenRequest("token-to-revoke");

        mockMvc.perform(post("/api/v1/auth/logout")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("Logged out successfully"));

        verify(authService).logout("token-to-revoke");
    }
}
