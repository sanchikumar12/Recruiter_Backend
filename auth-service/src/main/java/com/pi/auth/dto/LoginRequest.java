package com.pi.auth.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

@Schema(description = "Account login credentials")
public record LoginRequest(

        @Schema(description = "Registered email address", example = "candidate@example.com", requiredMode = Schema.RequiredMode.REQUIRED)
        @NotBlank
        @Email
        String email,

        @Schema(description = "Account password", example = "StrongPassword123!", requiredMode = Schema.RequiredMode.REQUIRED)
        @NotBlank
        String password
) {}
