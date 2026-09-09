package com.pi.user.controller;

import com.pi.user.dto.CreateUserRequest;
import com.pi.user.dto.UserResponse;
import com.pi.user.exception.ApiError;
import com.pi.user.service.UserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping({"/api/v1/users", "/api/candidates"})
@RequiredArgsConstructor
@Tag(name = "Candidate Profiles", description = "Operations for creating, querying, and managing candidate profiles in the 366PI Recruitment Platform")
public class UserController {

    private final UserService userService;

    @Operation(
            summary = "Create a new candidate profile",
            description = "Registers a candidate profile with personal, contact, and professional details. Rejects duplicate emails."
    )
    @ApiResponses(value = {
            @ApiResponse(
                    responseCode = "201",
                    description = "Candidate profile created successfully",
                    content = @Content(mediaType = "application/json", schema = @Schema(implementation = UserResponse.class))
            ),
            @ApiResponse(
                    responseCode = "400",
                    description = "Validation failure or email already exists",
                    content = @Content(mediaType = "application/json", schema = @Schema(implementation = ApiError.class))
            ),
            @ApiResponse(
                    responseCode = "500",
                    description = "Internal server error",
                    content = @Content(mediaType = "application/json", schema = @Schema(implementation = ApiError.class))
            )
    })
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public UserResponse createUser(
            @Valid @RequestBody CreateUserRequest request) {

        return userService.createUser(request);
    }

    @Operation(
            summary = "Retrieve candidate profile by ID",
            description = "Fetches the full candidate profile record using the candidate's unique UUID identifier."
    )
    @ApiResponses(value = {
            @ApiResponse(
                    responseCode = "200",
                    description = "Candidate profile retrieved successfully",
                    content = @Content(mediaType = "application/json", schema = @Schema(implementation = UserResponse.class))
            ),
            @ApiResponse(
                    responseCode = "404",
                    description = "Candidate not found with the specified UUID",
                    content = @Content(mediaType = "application/json", schema = @Schema(implementation = ApiError.class))
            ),
            @ApiResponse(
                    responseCode = "500",
                    description = "Internal server error",
                    content = @Content(mediaType = "application/json", schema = @Schema(implementation = ApiError.class))
            )
    })
    @GetMapping("/{id}")
    public UserResponse getUser(
            @Parameter(description = "Unique UUID of the candidate profile", example = "550e8400-e29b-41d4-a716-446655440000", required = true)
            @PathVariable UUID id) {

        return userService.getUser(id);
    }
}
