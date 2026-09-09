package com.pi.auth.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

@Schema(description = "Forgot password request payload to initiate password reset")
public record ForgotPasswordRequest(

        @Schema(description = "Registered user email address", example = "candidate@example.com", requiredMode = Schema.RequiredMode.REQUIRED)
        @NotBlank
        @Email
        String email
) {}
