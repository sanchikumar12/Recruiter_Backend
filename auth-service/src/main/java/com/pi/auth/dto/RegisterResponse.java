package com.pi.auth.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import java.util.UUID;

@Schema(description = "Account registration response containing onboarding details and credentials")
public record RegisterResponse(

        @Schema(description = "Success message", example = "User registered successfully.")
        String message,

        @Schema(description = "Registered email address", example = "user@example.com")
        String email,

        @Schema(description = "Password used for registration", example = "StrongPassword123!")
        String generatedPassword,

        @Schema(description = "Initial account status", example = "ACTIVE")
        String status,

        @Schema(description = "Assigned user role", example = "USER")
        String role,

        @Schema(description = "User unique account ID", example = "550e8400-e29b-41d4-a716-446655440000")
        UUID userId
) {
    public RegisterResponse(String message, String email, String generatedPassword, String status) {
        this(message, email, generatedPassword, status, "USER", null);
    }

    public RegisterResponse(String message, String email, String generatedPassword, String status, String role) {
        this(message, email, generatedPassword, status, role, null);
    }
}

