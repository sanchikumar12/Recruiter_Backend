package com.pi.user.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import io.swagger.v3.oas.annotations.media.Schema;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

@Schema(description = "Candidate profile response object")
public record UserResponse(

        @Schema(description = "Unique UUID identifier of the candidate profile", example = "550e8400-e29b-41d4-a716-446655440000")
        UUID id,

        @Schema(description = "Associated authentication account ID", example = "d3b07384-d113-40e9-9a29-d5964893992b", nullable = true)
        UUID authUserId,

        @Schema(description = "Candidate's full name", example = "Sanchit Kumar")
        String fullName,

        @Schema(description = "Candidate's mobile phone number", example = "+91 9999999999")
        String mobileNumber,

        @Schema(description = "Candidate's email address", example = "sanchit@example.com")
        String email,

        @Schema(description = "Candidate's date of birth (YYYY-MM-DD)", example = "1998-08-15")
        @JsonFormat(pattern = "yyyy-MM-dd")
        LocalDate dateOfBirth,

        @Schema(description = "Candidate's skills list", example = "[\"Java\", \"Spring Boot\", \"Microservices\", \"PostgreSQL\"]")
        List<String> skills,

        @Schema(description = "Candidate's location", example = "Ranchi, India")
        String location,

        @Schema(description = "Candidate's professional headline", example = "Senior Java Backend Engineer")
        String headline,

        @Schema(description = "Candidate's bio", example = "Software engineer experienced in distributed systems.")
        String bio,

        @Schema(description = "Candidate profile status", example = "ACTIVE", allowableValues = {"ACTIVE", "INACTIVE", "SUSPENDED"})
        String status,

        @Schema(description = "Profile creation timestamp", example = "2026-09-08T10:45:00Z")
        Instant createdAt,

        @Schema(description = "Last update timestamp", example = "2026-09-08T10:45:00Z")
        Instant updatedAt,

        @Schema(description = "User role", example = "USER")
        String role,

        @Schema(description = "Login password (provided or generated during registration)", example = "MySecretPass123")
        String password
) {
    public UserResponse(
            UUID id,
            UUID authUserId,
            String fullName,
            String mobileNumber,
            String email,
            LocalDate dateOfBirth,
            List<String> skills,
            String location,
            String headline,
            String bio,
            String status,
            Instant createdAt,
            Instant updatedAt
    ) {
        this(id, authUserId, fullName, mobileNumber, email, dateOfBirth, skills, location, headline, bio, status, createdAt, updatedAt, "USER", null);
    }
}
