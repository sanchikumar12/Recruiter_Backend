package com.pi.auth.controller;

import com.pi.auth.dto.ActivateAccountRequest;
import com.pi.auth.dto.AuthResponse;
import com.pi.auth.dto.ForgotPasswordRequest;
import com.pi.auth.dto.LoginRequest;
import com.pi.auth.dto.RefreshTokenRequest;
import com.pi.auth.dto.RegisterRequest;
import com.pi.auth.dto.RegisterResponse;
import com.pi.auth.dto.ResetPasswordRequest;
import com.pi.auth.service.AuthService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping({"/api/v1/auth", "/api/auth"})
@Tag(name = "Authentication & Authorization", description = "Endpoints for user registration, server-generated credentials, login, forgot/reset password, token refresh, and logout")
public class AuthController {

    private final AuthService authService;

    public AuthController(AuthService authService) {
        this.authService = authService;
    }

    @PostMapping("/register")
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Register candidate account", description = "Creates a new user account with CANDIDATE role and server-generated unique random password.")
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Account created successfully with generated login password"),
            @ApiResponse(responseCode = "400", description = "Validation error or invalid request payload"),
            @ApiResponse(responseCode = "409", description = "Email already registered")
    })
    public ResponseEntity<RegisterResponse> register(@Valid @RequestBody RegisterRequest request) {
        RegisterResponse response = authService.register(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @PostMapping("/activate")
    @Operation(summary = "Activate account", description = "Verifies activation token, sets account status to ACTIVE, and confirms password.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Account activated successfully"),
            @ApiResponse(responseCode = "400", description = "Invalid or expired activation token")
    })
    public ResponseEntity<Map<String, String>> activate(@Valid @RequestBody ActivateAccountRequest request) {
        authService.activate(request);
        return ResponseEntity.ok(Map.of("message", "Account successfully activated. You can now log in."));
    }

    @PostMapping("/login")
    @Operation(summary = "Authenticate user", description = "Authenticates user with email and password, issuing access JWT and rotatable refresh token.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Authenticated successfully"),
            @ApiResponse(responseCode = "401", description = "Invalid credentials or unactivated/locked account"),
            @ApiResponse(responseCode = "423", description = "Account temporarily locked due to excessive failed attempts")
    })
    public ResponseEntity<AuthResponse> login(@Valid @RequestBody LoginRequest request) {
        AuthResponse response = authService.login(request);
        return ResponseEntity.ok(response);
    }

    @PostMapping("/refresh")
    @Operation(summary = "Rotate refresh token & get new access token", description = "Revokes old refresh token and issues a new access token and rotated refresh token.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Token refreshed successfully"),
            @ApiResponse(responseCode = "401", description = "Invalid, expired, or revoked refresh token")
    })
    public ResponseEntity<AuthResponse> refresh(@Valid @RequestBody RefreshTokenRequest request) {
        AuthResponse response = authService.refresh(request);
        return ResponseEntity.ok(response);
    }

    @PostMapping("/forgot-password")
    @Operation(summary = "Forgot password", description = "Initiates password reset by issuing a 15-minute reset token.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Reset token generated if email exists")
    })
    public ResponseEntity<Map<String, String>> forgotPassword(@Valid @RequestBody ForgotPasswordRequest request) {
        String resetToken = authService.forgotPassword(request);
        Map<String, String> response = new HashMap<>();
        response.put("message", "If the email is registered, a password reset token has been issued.");
        if (resetToken != null) {
            response.put("resetToken", resetToken);
        }
        return ResponseEntity.ok(response);
    }

    @PostMapping("/reset-password")
    @Operation(summary = "Reset password", description = "Resets user password with valid reset token and revokes all previous refresh tokens.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Password reset successfully"),
            @ApiResponse(responseCode = "401", description = "Reset token invalid or expired")
    })
    public ResponseEntity<Map<String, String>> resetPassword(@Valid @RequestBody ResetPasswordRequest request) {
        authService.resetPassword(request);
        return ResponseEntity.ok(Map.of("message", "Password has been successfully reset. Please log in with your new password."));
    }

    @PostMapping("/logout")
    @Operation(summary = "Logout user", description = "Revokes the given refresh token to prevent further session continuation.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Logged out successfully")
    })
    public ResponseEntity<Map<String, String>> logout(@RequestBody(required = false) RefreshTokenRequest request) {
        if (request != null && request.refreshToken() != null) {
            authService.logout(request.refreshToken());
        }
        return ResponseEntity.ok(Map.of("message", "Logged out successfully"));
    }
}
