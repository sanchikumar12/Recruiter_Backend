package com.pi.user.mapper;

import com.pi.user.dto.UserResponse;
import com.pi.user.entity.User;
import org.springframework.stereotype.Component;

import java.util.ArrayList;

@Component
public class UserMapper {

    public UserResponse toResponse(User user) {
        return toResponse(user, null);
    }

    public UserResponse toResponse(User user, String password) {
        return new UserResponse(
                user.getId(),
                user.getAuthUserId(),
                user.getFullName(),
                user.getMobileNumber(),
                user.getEmail(),
                user.getDateOfBirth(),
                user.getSkills() != null ? new ArrayList<>(user.getSkills()) : new ArrayList<>(),
                user.getLocation(),
                user.getHeadline(),
                user.getBio(),
                user.getStatus().name(),
                user.getCreatedAt(),
                user.getUpdatedAt(),
                user.getRole() != null ? user.getRole() : "USER",
                password
        );
    }
}
