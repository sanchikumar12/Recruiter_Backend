package com.pi.user.client;

public record AuthRegisterRequest(
        String email,
        String password,
        String role
) {
}
