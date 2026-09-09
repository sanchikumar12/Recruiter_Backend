package com.pi.auth.dto;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Authentication response containing JWT tokens")
public record AuthResponse(

        @Schema(description = "Signed JWT access token", example = "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...")
        String accessToken,

        @Schema(description = "Opaque refresh token for session continuation", example = "e89c02d1-2531-417b-8395-e23e20eeb7b5")
        String refreshToken,

        @Schema(description = "Authentication scheme", example = "Bearer")
        String tokenType,

        @Schema(description = "Access token lifetime in seconds", example = "900")
        long expiresIn
) {}
