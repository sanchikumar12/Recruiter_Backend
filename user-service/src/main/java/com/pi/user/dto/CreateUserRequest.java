package com.pi.user.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import java.time.LocalDate;
import java.util.List;

@Schema(description = "Payload to register a new candidate profile")
public record CreateUserRequest(

        @Schema(description = "Candidate's full name", example = "Sanchit Kumar", requiredMode = Schema.RequiredMode.REQUIRED, maxLength = 200)
        @NotBlank
        @Size(max = 200)
        String fullName,

        @Schema(description = "Candidate's mobile phone number", example = "+91 9999999999", maxLength = 30)
        @Size(max = 30)
        String mobileNumber,

        @Schema(description = "Candidate's primary email address (must be unique)", example = "sanchit@example.com", requiredMode = Schema.RequiredMode.REQUIRED, maxLength = 255)
        @NotBlank
        @Email
        @Size(max = 255)
        String email,

        @Schema(description = "Candidate's date of birth (YYYY-MM-DD)", example = "1998-08-15")
        @JsonFormat(pattern = "yyyy-MM-dd")
        LocalDate dateOfBirth,

        @Schema(description = "Candidate's skill section: list or array of skills selected or entered from frontend", example = "[\"Java\", \"Spring Boot\", \"Microservices\", \"PostgreSQL\", \"Docker\"]")
        List<String> skills,

        @Schema(description = "Candidate's current location/city", example = "Ranchi, India", maxLength = 255)
        @Size(max = 255)
        String location,

        @Schema(description = "Professional headline", example = "Senior Java Backend Engineer", maxLength = 255)
        @Size(max = 255)
        String headline,

        @Schema(description = "Short biography or summary", example = "Experienced in microservices architecture", maxLength = 2000)
        @Size(max = 2000)
        String bio,

        @Schema(description = "User role: USER, ADMIN, SUPERADMIN, CANDIDATE, RECRUITER", example = "USER", defaultValue = "USER")
        String role,

        @Schema(description = "Custom login password (optional; if omitted, an alphanumeric password is generated)", example = "MySecretPass123")
        String password
) {
    public CreateUserRequest(
            String fullName,
            String mobileNumber,
            String email,
            LocalDate dateOfBirth,
            List<String> skills,
            String location,
            String headline,
            String bio
    ) {
        this(fullName, mobileNumber, email, dateOfBirth, skills, location, headline, bio, "USER", null);
    }
}
