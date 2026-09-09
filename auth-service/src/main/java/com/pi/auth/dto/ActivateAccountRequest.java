package com.pi.auth.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

@Schema(description = "Account activation and password confirmation payload")
public record ActivateAccountRequest(

        @Schema(description = "Activation token received via email/notification", example = "9b1deb4d-3b7d-4bad-9bdd-2b0d7b3dcb6d", requiredMode = Schema.RequiredMode.REQUIRED)
        @NotBlank
        String token,

        @Schema(description = "Account password to set (min 8 characters)", example = "StrongPassword123!", requiredMode = Schema.RequiredMode.REQUIRED, minLength = 8, maxLength = 100)
        @NotBlank
        @Size(min = 8, max = 100)
        String password
) {}
