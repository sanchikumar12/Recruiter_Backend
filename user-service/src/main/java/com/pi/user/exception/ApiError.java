package com.pi.user.exception;

import io.swagger.v3.oas.annotations.media.Schema;

import java.time.Instant;

@Schema(description = "Standardized error response")
public record ApiError(

        @Schema(description = "Error timestamp", example = "2026-09-08T10:45:00Z")
        Instant timestamp,

        @Schema(description = "HTTP status code", example = "400")
        int status,

        @Schema(description = "Business error code", example = "VALIDATION_FAILED")
        String code,

        @Schema(description = "Human-readable error explanation", example = "email: must be a well-formed email address")
        String message
) {
}
