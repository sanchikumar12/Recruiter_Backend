package com.pi.auth.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

@Schema(description = "Reset password payload with reset token and new password")
public record ResetPasswordRequest(

        @Schema(description = "Password reset token received via email/notification", example = "a2d4f8b9-1234-5678-9abc-def012345678", requiredMode = Schema.RequiredMode.REQUIRED)
        @NotBlank
        String token,

        @Schema(description = "New secure password (min 8 characters)", example = "NewStrongPassword123!", requiredMode = Schema.RequiredMode.REQUIRED, minLength = 8, maxLength = 100)
        @NotBlank
        @Size(min = 8, max = 100)
        String newPassword
) {}
