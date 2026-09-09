package com.pi.auth.dto;

import com.pi.auth.enums.Role;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

@Schema(description = "Account registration request payload. Password can be provided by user or auto-generated if omitted.")
public record RegisterRequest(

        @Schema(description = "User primary email address", example = "user@example.com", requiredMode = Schema.RequiredMode.REQUIRED)
        @NotBlank
        @Email
        String email,

        @Schema(description = "Optional account password. If provided, user logs in with this password. If omitted, a secure random password is generated.", example = "StrongPassword123!", requiredMode = Schema.RequiredMode.NOT_REQUIRED)
        @Size(min = 6, max = 100)
        String password,

        @Schema(description = "User role (USER, ADMIN, SUPERADMIN, CANDIDATE, RECRUITER). Defaults to USER if omitted.", example = "USER")
        Role role
) {
    public RegisterRequest(String email, String password) {
        this(email, password, null);
    }
}
