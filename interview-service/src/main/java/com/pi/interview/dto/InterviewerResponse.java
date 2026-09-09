package com.pi.interview.dto;

import java.time.LocalDateTime;
import java.util.UUID;

public record InterviewerResponse(
        UUID id,
        UUID userId,
        String name,
        String email,
        boolean active,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {
}
