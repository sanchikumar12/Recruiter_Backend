package com.pi.auth.exception;

import io.swagger.v3.oas.annotations.media.Schema;

import java.time.Instant;

@Schema(description = "Standardized error response payload")
public record ApiError(

        @Schema(description = "Timestamp when error occurred", example = "2026-09-08T11:00:00Z")
        Instant timestamp,

        @Schema(description = "HTTP status code", example = "400")
        int status,

        @Schema(description = "Structured business error code", example = "INVALID_CREDENTIALS")
        String code,

        @Schema(description = "Human-readable explanation of the error", example = "Invalid credentials")
        String message
) {}
