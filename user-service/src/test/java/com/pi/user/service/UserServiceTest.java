package com.pi.user.service;

import com.pi.user.client.AuthRegisterResponse;
import com.pi.user.client.AuthServiceClient;
import com.pi.user.dto.CreateUserRequest;
import com.pi.user.dto.UserResponse;
import com.pi.user.entity.ProfileStatus;
import com.pi.user.entity.User;
import com.pi.user.exception.UserNotFoundException;
import com.pi.user.mapper.UserMapper;
import com.pi.user.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class UserServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private AuthServiceClient authServiceClient;

    @Spy
    private UserMapper userMapper = new UserMapper();

    @InjectMocks
    private UserServiceImpl userService;

    private CreateUserRequest sampleRequest;

    @BeforeEach
    void setUp() {
        userService = new UserServiceImpl(userRepository, userMapper, authServiceClient);
        sampleRequest = new CreateUserRequest(
                "Sanchit Kumar",
                "+91 9999999999",
                "sanchit@example.com",
                LocalDate.of(1998, 8, 15),
                List.of("Java", "Spring Boot", "Microservices", "PostgreSQL"),
                "Ranchi",
                "Java Backend Developer",
                "Software engineer interested in microservices."
        );
    }

    @Test
    @DisplayName("Should successfully create a user when email is unique")
    void shouldCreateUserSuccessfully() {
        UUID authUserId = UUID.randomUUID();
        when(userRepository.existsByEmail("sanchit@example.com")).thenReturn(false);
        when(authServiceClient.register(any(), any(), any()))
                .thenReturn(new AuthRegisterResponse("User registered successfully.", "sanchit@example.com", "AutoPass123", "ACTIVE", "USER", authUserId));
        when(userRepository.save(any(User.class))).thenAnswer(invocation -> invocation.getArgument(0));

        UserResponse response = userService.createUser(sampleRequest);

        assertThat(response).isNotNull();
        assertThat(response.id()).isNotNull();
        assertThat(response.authUserId()).isEqualTo(authUserId);
        assertThat(response.fullName()).isEqualTo("Sanchit Kumar");
        assertThat(response.mobileNumber()).isEqualTo("+91 9999999999");
        assertThat(response.email()).isEqualTo("sanchit@example.com");
        assertThat(response.dateOfBirth()).isEqualTo(LocalDate.of(1998, 8, 15));
        assertThat(response.skills()).containsExactly("Java", "Spring Boot", "Microservices", "PostgreSQL");
        assertThat(response.status()).isEqualTo("ACTIVE");
        assertThat(response.role()).isEqualTo("USER");
        assertThat(response.password()).isEqualTo("AutoPass123");
        assertThat(response.createdAt()).isNotNull();

        verify(userRepository, times(1)).existsByEmail("sanchit@example.com");
        verify(authServiceClient, times(1)).register(eq("sanchit@example.com"), isNull(), eq("USER"));
        verify(userRepository, times(1)).save(any(User.class));
    }

    @Test
    @DisplayName("Should throw IllegalArgumentException when email already exists")
    void shouldThrowExceptionWhenEmailAlreadyExists() {
        when(userRepository.existsByEmail("sanchit@example.com")).thenReturn(true);

        assertThatThrownBy(() -> userService.createUser(sampleRequest))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Email already exists");

        verify(userRepository, times(1)).existsByEmail("sanchit@example.com");
        verify(userRepository, never()).save(any());
    }

    @Test
    @DisplayName("Should return user response when user exists by ID")
    void shouldReturnUserWhenExists() {
        UUID userId = UUID.randomUUID();
        User user = new User();
        user.setId(userId);
        user.setFullName("Sanchit Kumar");
        user.setMobileNumber("+91 9999999999");
        user.setEmail("sanchit@example.com");
        user.setDateOfBirth(LocalDate.of(1998, 8, 15));
        user.setSkills(List.of("Java", "Spring Boot"));
        user.setStatus(ProfileStatus.ACTIVE);
        user.setCreatedAt(Instant.now());
        user.setUpdatedAt(Instant.now());

        when(userRepository.findById(userId)).thenReturn(Optional.of(user));

        UserResponse response = userService.getUser(userId);

        assertThat(response).isNotNull();
        assertThat(response.id()).isEqualTo(userId);
        assertThat(response.fullName()).isEqualTo("Sanchit Kumar");
        assertThat(response.email()).isEqualTo("sanchit@example.com");
        assertThat(response.skills()).containsExactly("Java", "Spring Boot");
        verify(userRepository, times(1)).findById(userId);
    }

    @Test
    @DisplayName("Should throw UserNotFoundException when user does not exist")
    void shouldThrowExceptionWhenUserNotFound() {
        UUID userId = UUID.randomUUID();
        when(userRepository.findById(userId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> userService.getUser(userId))
                .isInstanceOf(UserNotFoundException.class)
                .hasMessageContaining("User not found");

        verify(userRepository, times(1)).findById(userId);
    }
}
