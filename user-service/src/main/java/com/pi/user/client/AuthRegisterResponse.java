package com.pi.user.client;

import java.util.UUID;

public record AuthRegisterResponse(
        String message,
        String email,
        String generatedPassword,
        String status,
        String role,
        UUID userId
) {
}
