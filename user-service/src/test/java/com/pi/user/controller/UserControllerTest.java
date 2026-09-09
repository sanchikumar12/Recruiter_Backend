package com.pi.user.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.pi.user.dto.CreateUserRequest;
import com.pi.user.dto.UserResponse;
import com.pi.user.exception.GlobalExceptionHandler;
import com.pi.user.exception.UserNotFoundException;
import com.pi.user.service.UserService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(UserController.class)
@Import(GlobalExceptionHandler.class)
class UserControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private UserService userService;

    @Test
    @DisplayName("POST /api/v1/users - Should return 201 when request is valid")
    void shouldReturn201WhenValidRequest() throws Exception {
        UUID id = UUID.randomUUID();
        CreateUserRequest request = new CreateUserRequest(
                "Sanchit Kumar",
                "+91 9999999999",
                "sanchit@example.com",
                LocalDate.of(1998, 8, 15),
                List.of("Java", "Spring Boot", "Microservices"),
                "Ranchi",
                "Java Backend Developer",
                "Bio description"
        );

        UserResponse response = new UserResponse(
                id,
                null,
                "Sanchit Kumar",
                "+91 9999999999",
                "sanchit@example.com",
                LocalDate.of(1998, 8, 15),
                List.of("Java", "Spring Boot", "Microservices"),
                "Ranchi",
                "Java Backend Developer",
                "Bio description",
                "ACTIVE",
                Instant.now(),
                Instant.now()
        );

        when(userService.createUser(any(CreateUserRequest.class))).thenReturn(response);

        mockMvc.perform(post("/api/v1/users")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(id.toString()))
                .andExpect(jsonPath("$.fullName").value("Sanchit Kumar"))
                .andExpect(jsonPath("$.mobileNumber").value("+91 9999999999"))
                .andExpect(jsonPath("$.email").value("sanchit@example.com"))
                .andExpect(jsonPath("$.dateOfBirth").value("1998-08-15"))
                .andExpect(jsonPath("$.skills[0]").value("Java"))
                .andExpect(jsonPath("$.status").value("ACTIVE"));
    }

    @Test
    @DisplayName("POST /api/v1/users - Should return 400 when required fields are missing")
    void shouldReturn400WhenInvalidPayload() throws Exception {
        CreateUserRequest invalidRequest = new CreateUserRequest(
                "",
                null,
                "invalid-email",
                null,
                null,
                null,
                null,
                null
        );

        mockMvc.perform(post("/api/v1/users")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(invalidRequest)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VALIDATION_FAILED"));
    }

    @Test
    @DisplayName("POST /api/v1/users - Should return 400 when email already exists")
    void shouldReturn400WhenDuplicateEmail() throws Exception {
        CreateUserRequest request = new CreateUserRequest(
                "Sanchit Kumar",
                "+91 9999999999",
                "sanchit@example.com",
                LocalDate.of(1998, 8, 15),
                List.of("Java"),
                null,
                null,
                null
        );

        when(userService.createUser(any(CreateUserRequest.class)))
                .thenThrow(new IllegalArgumentException("Email already exists"));

        mockMvc.perform(post("/api/v1/users")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("BAD_REQUEST"))
                .andExpect(jsonPath("$.message").value("Email already exists"));
    }

    @Test
    @DisplayName("GET /api/v1/users/{id} - Should return 200 when user exists")
    void shouldReturn200WhenUserExists() throws Exception {
        UUID id = UUID.randomUUID();
        UserResponse response = new UserResponse(
                id,
                null,
                "Sanchit Kumar",
                "+91 9999999999",
                "sanchit@example.com",
                LocalDate.of(1998, 8, 15),
                List.of("Java", "Spring Boot"),
                "Ranchi",
                "Headline",
                "Bio",
                "ACTIVE",
                Instant.now(),
                Instant.now()
        );

        when(userService.getUser(id)).thenReturn(response);

        mockMvc.perform(get("/api/v1/users/{id}", id))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(id.toString()))
                .andExpect(jsonPath("$.fullName").value("Sanchit Kumar"))
                .andExpect(jsonPath("$.email").value("sanchit@example.com"))
                .andExpect(jsonPath("$.skills[0]").value("Java"));
    }

    @Test
    @DisplayName("GET /api/v1/users/{id} - Should return 404 when user not found")
    void shouldReturn404WhenUserNotFound() throws Exception {
        UUID id = UUID.randomUUID();
        when(userService.getUser(id)).thenThrow(new UserNotFoundException("User not found: " + id));

        mockMvc.perform(get("/api/v1/users/{id}", id))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("USER_NOT_FOUND"));
    }
}
