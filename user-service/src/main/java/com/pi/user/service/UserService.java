package com.pi.user.service;

import com.pi.user.dto.CreateUserRequest;
import com.pi.user.dto.UserResponse;

import java.util.UUID;

public interface UserService {

    UserResponse createUser(CreateUserRequest request);

    UserResponse getUser(UUID id);
}
