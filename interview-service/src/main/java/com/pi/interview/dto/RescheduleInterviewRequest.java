package com.pi.interview.dto;

import jakarta.validation.constraints.NotNull;

import java.util.UUID;

public record RescheduleInterviewRequest(
        @NotNull(message = "New slot ID is required")
        UUID newSlotId
) {
}
