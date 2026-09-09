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
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.ArrayList;
import java.util.UUID;

@Service
@Transactional(readOnly = true)
public class UserServiceImpl implements UserService {

    private final UserRepository userRepository;
    private final UserMapper userMapper;
    private final AuthServiceClient authServiceClient;

    public UserServiceImpl(UserRepository userRepository, UserMapper userMapper, AuthServiceClient authServiceClient) {
        this.userRepository = userRepository;
        this.userMapper = userMapper;
        this.authServiceClient = authServiceClient;
    }

    @Override
    @Transactional
    public UserResponse createUser(CreateUserRequest request) {
        String email = request.email().trim().toLowerCase();

        if (userRepository.existsByEmail(email)) {
            throw new IllegalArgumentException("Email already exists");
        }

        String role = (request.role() != null && !request.role().isBlank()) ? request.role() : "USER";

        // Register credentials in auth-service
        AuthRegisterResponse authResponse = null;
        if (authServiceClient != null) {
            authResponse = authServiceClient.register(email, request.password(), role);
        }

        Instant now = Instant.now();

        User user = new User();
        user.setId(UUID.randomUUID());
        if (authResponse != null && authResponse.userId() != null) {
            user.setAuthUserId(authResponse.userId());
        }
        user.setFullName(request.fullName());
        user.setMobileNumber(request.mobileNumber());
        user.setEmail(email);
        user.setDateOfBirth(request.dateOfBirth());
        user.setSkills(request.skills() != null ? new ArrayList<>(request.skills()) : new ArrayList<>());
        user.setLocation(request.location());
        user.setHeadline(request.headline());
        user.setBio(request.bio());
        user.setStatus(ProfileStatus.ACTIVE);
        user.setRole(authResponse != null && authResponse.role() != null ? authResponse.role() : role);
        user.setCreatedAt(now);
        user.setUpdatedAt(now);

        User savedUser = userRepository.save(user);

        String returnedPassword = authResponse != null ? authResponse.generatedPassword() : request.password();
        return userMapper.toResponse(savedUser, returnedPassword);
    }

    @Override
    public UserResponse getUser(UUID id) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new UserNotFoundException("User not found: " + id));

        return userMapper.toResponse(user);
    }
}

